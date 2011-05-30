package recibo.app.billshare;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import recibo.app.billshare.R;
import recibo.platform.ReciboContentProvider;
import recibo.platform.model.Item;
import recibo.platform.model.Receipt;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;


public class Billshare extends Activity{
	
	private static final String TAG = "debug";
	private static final int ADD_USER_DIALOG = 1;
	private static final int MUST_ADD_USER_DIALOG = 2;
	private static final int ALREADY_TAKEN_DIALOG = 3;
	private static final int BILLSHARE_DUMMY_QUERY = 0;
	
	private static final DecimalFormat priceFormatter = new DecimalFormat("$#0.00");
	
	private ArrayList <User> users = new ArrayList<User>();
	private User activeUser = null;
	private Receipt activeReceipt = null;
	HashMap<Integer, String> accountedItems = new HashMap<Integer, String>();
	
	//TODO Allow items to be split across multiple users
	
	//this class is for a user who has been added to a receipt.
	//
	//variables:
	//Item[] items			for the items user is responsible for paying for
	//string name			the user's name and identifier
	private static class User{
		
		ArrayList <Item> items = new ArrayList<Item>();
		public final String name;
		public final int color;
		
		private static final ArrayList<Integer> colors = new ArrayList<Integer>();
		
		static {
			colors.add(0xFFFFECEC);		//pink
			colors.add(0xFFCBC5F5);		//purple
			colors.add(0xFFDBF0F7);		//blue
			colors.add(0xFFF5F7C4);		//yellow
		}
		 
		private Integer getNewColor() {
			return colors.remove(0);
		}
	
		public User(String name){
			this.name=name;
			this.color=getNewColor();
		}
		
		//add an item to a given user.
		public void addItem(Item i){
			this.items.add(i);			
		}
		
		public void removeItem(Item i){
			this.items.remove(this.items.indexOf(i));			
		}
		
		public boolean hasItem(Item i){
			return items.contains(i);			
		}
		
		public String getName(){
			return this.name;
		}
		
	}
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.billshare_receiptview);
    	    	
    	Cursor rcp = ReciboContentProvider.dummyQuery(BILLSHARE_DUMMY_QUERY); 
        rcp.moveToPosition(0);
        activeReceipt = new Receipt(rcp);    	
    	
        
        ListView lv = (ListView)findViewById(R.id.listview);
        String[] from = new String[] {"name", "price", "ID"};
        int[] to = new int[] { R.id.item_name, R.id.item_price, R.id.item_id};
        
        List<HashMap<String, String>> fillMaps = new ArrayList<HashMap<String, String>>();
        for (int i = 0; i < activeReceipt.items.length; i++){
        	HashMap<String, String> map = new HashMap<String, String>();
        	
        	map.put("name", activeReceipt.items[i].name);
        	map.put("price", priceFormatter.format(activeReceipt.items[i].price));
        	map.put("ID", Integer.toString(activeReceipt.items[i]._id));
        	fillMaps.add(map);
        }
        
        // fill in the grid_item layout
        SimpleAdapter adapter = new SimpleAdapter(this, fillMaps, R.layout.grid_item, from, to);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(tableRowClickListener);        		
        		
        
        final Button button = (Button) findViewById(R.id.add_user_button);
        button.setOnClickListener(buttonClickListener);      

        //private JunctionMaker mJunctionMaker;
        
    }

    private View.OnClickListener buttonClickListener = new View.OnClickListener() {
		
    	 public void onClick(View v) {
    		 showDialog(ADD_USER_DIALOG); //set the active user
    		 for(int j=0; j < activeReceipt.items.length; j++){
    			 Item i = activeReceipt.items[j];    			 
    		 }
         }
    	 
    	 
     };
    
   
    private OnItemClickListener tableRowClickListener = new OnItemClickListener(){
    	public void onItemClick (AdapterView parent, View v, int position, long id){

    		if (activeUser == null) showDialog(MUST_ADD_USER_DIALOG); //insist on at least one user
    		else
    		{	

    			TextView tv = (TextView)(((LinearLayout)v).findViewById(R.id.item_id));
    			Item i = getItemByID(Integer.parseInt((tv).getText().toString()));
    			View row = (View) tv.getParent();

    			if (i != null)
    			{
    				//add to accounted for items map.
    				if (!accountedItems.containsKey(i._id))
    				{
    					//add item
    					activeUser.addItem(i);
    					accountedItems.put(i._id, activeUser.getName());
    					row.setBackgroundColor(activeUser.color);
    					
    					//check if everything is taken
    					if (accountedItems.keySet().size() == activeReceipt.items.length)
    					{
    						//TODO: compute total
    						//switch view and change totals by person
    						//
    						//gridlayout.addview(tableLayout1)
    						//gridLayout.removeView(tableLayout1)
    						//replace new view

    					}
    				}
    				else
    				{
    					if (activeUser.hasItem(i)){
    						activeUser.removeItem(i);
    						accountedItems.remove(i._id);
    						row.setBackgroundColor(0xfff3fbe9);
    						
    					}
    					else
    					{
    						showDialog(ALREADY_TAKEN_DIALOG);
    					}
    				}
    			}
    			
    			double total = 0;
    			for (int j=0; j < activeUser.items.size(); j++)
    			{
    				total += activeUser.items.get(j).price;
    			}
    			TextView totalView = (TextView)findViewById(R.id.active_user_total);
    			totalView.setText(activeUser.getName() + "'s total: " + priceFormatter.format(total));
    		}
    		/*
	    	}

	    	else
	    	{
	    		throw new RuntimeException("Unrecognized click.");
	    	}
    		 */	    	
    	}
    };
    
    public Dialog onCreateDialog(int id, Bundle args) {
    	switch (id) {
    	case ADD_USER_DIALOG:
    		return createAddUserDialog();
    	case MUST_ADD_USER_DIALOG:
    		return createMustAddDialog();
    	case ALREADY_TAKEN_DIALOG:
    		return createItemAlreadyAccountedDialog();
    		default:
    			throw new RuntimeException("Dialog creation failed");
    	}
    }
    
    private Dialog createItemAlreadyAccountedDialog(){
    	AlertDialog.Builder alert = new AlertDialog.Builder(this);
    	alert.setTitle("Error");
    	alert.setMessage("That item is already accounted for.");
    	alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();	
			}
		});
    	return alert.create();
    }
    
    private Dialog createMustAddDialog() {
    	AlertDialog.Builder alert = new AlertDialog.Builder(this);
    	alert.setTitle("Error");
    	alert.setMessage("Must add user before selecting items.");
    	alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();	
			}
		});
    	return alert.create();
    }
    
  //create add user dialog
    private Dialog createAddUserDialog() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
    	alert.setTitle("Add person");
    	alert.setMessage("Enter name:");
	    final EditText input = new EditText(this);		
		alert.setView(input);
		
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			  String value = input.getText().toString();
			  User u = new User(value);
			  users.add(u);
			  activeUser = u;
			  
			  TextView tv = (TextView)findViewById(R.id.active_user);
			  tv.setText("claiming items for: " + activeUser.getName());
			  tv = (TextView)findViewById(R.id.active_user_total);
			  tv.setText(activeUser.getName() + "'s total: $0.00");
			  }
			});
	
			alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			  public void onClick(DialogInterface dialog, int whichButton) {
			    // Cancelled.
			  }
			});
		
		return alert.create();
    }
    
    private Item getItemByID(int itemID){
        for (int i = 0; i < activeReceipt.items.length; i++){
        	if (itemID == activeReceipt.items[i]._id)
        		return activeReceipt.items[i];
        }
        return null;   	
    }    
}



