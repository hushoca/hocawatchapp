package com.hhoca.hocawatch;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;


/***
 * ON THE ANDROID MANIFEST android:screenOrientation="portrait" IS ADDED TO MAKE SURE
 * ACTIVITY IS NOT KILLED WHILE SEARCHING... <RANT START> I love Google (pls hire me) but i find it dumb
 * that when you go from portrait to landspace the activity is killed and recreated...
 * Come on google you have millions... you couldnt just idk add a onActivityRotated() function?
 * So my dumb activity doesnt get killed...<RANT END>
 * @author hhoca
 *
 */
public class MainActivity extends ActionBarActivity {

	//VARIABLE DEFINITIONS
	IntentFilter				_intentFilter	=	new IntentFilter();
	BluetoothAdapter			_btAdapter		=	BluetoothAdapter.getDefaultAdapter();
	static boolean				_isDiscovering	=	false;
	ArrayList<BluetoothDevice>	_foundDevices	=	new ArrayList<BluetoothDevice>();
	ProgressDialog dialogSearchingForDevices;
	
	//IMPLEMENT EVENTS
	/***
	 * RESPONSIBLE FOR discoveryStarted,DeviceFound,discoveryStopped
	 * [CALLED WHEN ONE OF THESE IS RECEIVED]
	 */
	BroadcastReceiver 	_broadcastReceiver 	=	new BroadcastReceiver() {

		/***
		 * WHEN DISCOVERY STARTED => CLEAR LIST OF DEVICES => SHOW SEARCHING
		 * DIALOG.
		 * @param context
		 * @param intent
		 */
		private void discoveryStarted(Context context, Intent intent){
			Log.d("debugging", "disc started");
			if(_isDiscovering){
				_foundDevices.clear();	//clear list of devices so the previous scan doesnt stick
				showSearcingBTDevicesDialog();	//show searching dialog
			}
		}
		
		/***
		 * WHEN DISCOVERY IS FINISHED => LIST THE FOUND DEVICES ON A 
		 * DIALOG
		 * @param context 	= Context
		 * @param intent	= Intent
		 */
		private void discoveryStopped(Context context, Intent intent){
			if(_isDiscovering){
				_isDiscovering = false;		//set discovery finished
				dialogSearchingForDevices.cancel();	//close searching dialog
				if(_btAdapter.isEnabled()){	//if bluetooth is still on 
					showListofFoundBTDevicesDialog();	//show list the devices found
				}else{	//if user closed the bluetooth. show error message:
					Toast.makeText(getApplicationContext(), "Bluetooth is turned off! Scanning stopped.", Toast.LENGTH_SHORT).show();
				}
			}	
		}
		
		/***
		 * [CALLED WHEN DEVICE IS FOUND] GET DEVICE FOUND AND PUT IT
		 * INTO THE LISTARRAY FOR FURTHER USE. 
		 */
		private void deviceFound(Context context, Intent intent){
			if(_isDiscovering){	//if discovery is called by this app	
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);//get found device
				if(device.getName() != null) {	//make sure device is not a null (for some reason it happens.)
					_foundDevices.add(device);//add found device to the _foundDevices list
				}
			}
		}
		
		/***
		 * [CALLED WHEN THE ACTIONS ABOVE HAPPENED] Calls the according
		 * function for action.
		 */
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			
			String action = arg1.getAction();	//get action string...
			
			if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)){
				discoveryStarted(arg0, arg1);
			}
			
			if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)){
				discoveryStopped(arg0, arg1);
			}
			
			if(action.equals(BluetoothDevice.ACTION_FOUND)){
				deviceFound(arg0, arg1);
			}
			
		}
		
	};
	
	/***
	 * SHOW SEACHING FOR DEVICES DIALOG
	 * [SELF EXPLANATORY CODE]
	 */
	protected void showSearcingBTDevicesDialog(){
		dialogSearchingForDevices = new ProgressDialog(MainActivity.this);
		dialogSearchingForDevices.setProgressStyle(ProgressDialog.STYLE_SPINNER);	//set type to spinny spinny
		dialogSearchingForDevices.setMessage("Searching for Bluetooth devices! Please Wait...");	//message to show
		dialogSearchingForDevices.setIndeterminate(true);
		dialogSearchingForDevices.setCanceledOnTouchOutside(false);//make sure does not get killed
		dialogSearchingForDevices.show();	//show dialog!
	} 
	
	protected void showListofFoundBTDevicesDialog(){
		
		//init dialog builder and array adapter for builder
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_item); //select type of dialog
		
		for (BluetoothDevice device: _foundDevices) {					//add found devices to the list with the 
			arrayAdapter.add(device.getName()+"\n"+device.getAddress());//format: 	NAME
		}																//			ADDRESS
				
		if(!_foundDevices.isEmpty() /*&& _btAdapter.isEnabled()*/){ //check to see which dialog to create
			//CREATE LIST OF DEVICES DIALOG
			dialogBuilder.setTitle("Please select the Hoca Watch:");	//title
			dialogBuilder.setIcon(R.drawable.ic_launcher);				//set icon
			
			//adapter listener
			dialogBuilder.setAdapter(arrayAdapter, new OnClickListener() {		
					
				@Override
				public void onClick(DialogInterface arg0, int index) {//get triggered when clicked 
					//get the index of the button clicked => put that index into the _foundDevices.get()
					//which will return the BTdevice with that index.
					BluetoothDevice device = _foundDevices.get(index);
					//show which device is clicked.
					Toast.makeText(getApplicationContext(), "Connecting to "+device.getName(), Toast.LENGTH_LONG).show();
					
					//setup intent to start the service. Give it the BT_DEVICE extra which contains the
					//selected bluetoothdevice info.
					Intent startBTService = new Intent(getApplicationContext(),BluetoothService.class);
					startBTService.putExtra("BT_DEVICE", device);
					startService(startBTService);
				}
					
			});
		}else{
			dialogBuilder.setTitle("Hoca Watch Warning!");
			dialogBuilder.setIcon(R.drawable.ic_launcher);
			dialogBuilder.setMessage("No Bluetooth devices are found!"
					+ "\nPlease check that your Hoca Watch is turned on and scan the devices again."
					+ "\nTo close this click outside of this message.");
		}
		
		AlertDialog dialog = dialogBuilder.create();
		dialog.show();
	}
	
	/***
	 * [CALLED WHEN THE ACTIVITY IS CREATED. CALLED BEFORE CREATING AND THIS CREATES
	 * THE ACTIVITY BY setContentView]
	 */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        //Check if bluetooth is supported on this device.
        if(_btAdapter == null){
        	Toast.makeText(getApplicationContext(), "Bluetooth is not supported on this device!", Toast.LENGTH_SHORT).show();
        	//TODO INSERT KILL ACTIVITY COMMAND.
        }
        
        //SET INTENT FILTERS
        _intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        _intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        _intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        
        //REGISTER BROADCAST RECEIVERS
        registerReceiver(_broadcastReceiver, _intentFilter);
        
        //TODO main implementation.
    }
    
    /***
     * GETS CALLED WHEN ACTIVITY IS DESTROYED.
     */
    @Override
    protected void onDestroy() {
    	//unregister receivers to prevent leakage
    	unregisterReceiver(_broadcastReceiver);
    	super.onDestroy();
    }
    
    /***
     * USED TO POPULATE MENU ON THE RIGHT TOP SIDE OF THE ACTIVITY.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
    	return true;
    }
    
    /***
     * [CALLED WHEN USER OPENS MENU ON THE TOP RIGHT.CALLED BEFORE MENU IS CREATED]
     * CHECKS THE SERVICE CONNECTED/RUNNING STATUS AND CHANGES THE BUTTON
     * AVAILABILITY ACCORDINGLY.
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	
    	//GET MENU ITEMS SO THEY CAN BE ENABLED AND DISABLED
    	MenuItem connectButton = menu.findItem(R.id.menu_connect_button);	
    	MenuItem stopButton = menu.findItem(R.id.menu_disconnect_button);
    	MenuItem settingsButton = menu.findItem(R.id.menu_settings_button);
    	
    	//IF CONNECTED
    	if(BluetoothService.isRunning){
        	connectButton.setEnabled(false);	//DISABLE CONNECT BUTTON (so user cant rescan while connected)
        	stopButton.setEnabled(true);		//ENABLE DISCONNECT BUTTON (so user can disconnect)
        	settingsButton.setEnabled(false);	//DISABLE SETTINGS BUTTON (so user cant change settings while
        										//service is still running)
        }else{	//IF NOT CONNECTED
        	connectButton.setEnabled(true);		//ENABLE CONNECT BUTTON	(so user can re connect)
        	stopButton.setEnabled(false);		//DISABLE DISCONNECT BUTTON	(user is not connected anyways...)
        	settingsButton.setEnabled(true);	//ENABLE SETTINGS BUTTON (so settings can be changed)
        }
    	return super.onPrepareOptionsMenu(menu);
    }

    /////////////////////////////////////////////////////////////////////////////
    /***
     * ATTEMPT TO START DISCOVERY.
     * IF BT IS OFF => SEND TURN ON REQUEST:
     * 		IF STILL OF => GIVE ERROR
     * 		IF TURNED ON=>	startDiscovery();
     */
    public void attemptConnectBT(){
    	//check if BT is enabled
    	if(_btAdapter.isEnabled()){
     		_isDiscovering = true;	//set discovering true so we can check it later to see we are the one discovering
     								//not some other app.
    		_btAdapter.startDiscovery();//start bt discovery.
    	}else{
    		//create and send action_request_enable. which will return requestCode = 1
    		//and result -1 if connected successfully to the onActivityResult
    		Intent turnOnBTRequest = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    		startActivityForResult(turnOnBTRequest, 1);
    	}
     }
    
    /***
     * FUNCTION THAT DISCONNECTS THE WATCH FROM PHONE
     * [KILLS SERVICE => SERVICE STOPS THE CONNECTION BEFORE DYING]
     */
    public void disconnectBT(){
    	if(BluetoothService.isRunning){
    		//setup intent for service.
    		Intent bluetoothService = new Intent(getApplicationContext(), BluetoothService.class);
    		stopService(bluetoothService);//kill the service.
    	}
    }
    /////////////////////////////////////////////////////////////////////////////
    /***
     * [CALLED WHEN USER CLICKS A BUTTON ON THE TOP RIGHT MENU]
     * CHECK WHICH BUTTON IS CLICKED ON THE MENU AND DO CALL THE REQUIRED
     * FUNCTION BY THAT BUTTON.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();//get the id of the button clicked
        switch(id) {
			case R.id.menu_connect_button://connect button is clicked
				attemptConnectBT();		//attempt to connect to BT
				Log.d("debugging", "line 253");
				break;
			case R.id.menu_disconnect_button://disconnect button is clicked
				disconnectBT();			   //disconnect from device
				break;
			case R.id.menu_settings_button:	//settings button is clicked
				//TODO OPEN OPTIONS MENU
				break;
			default:
				break;
		}
        return super.onOptionsItemSelected(item);
    }
    
    /***
     * [CALLED WHEN startActivityForResult RETURNS A RESULT]
     * CHECK IF BT IS TURNED ON SUCCESSFULLY AFTER REQUEST. IF TURNED ON CALL startDiscovery()
     * IF NOT ON SHOW ERROR MESSAGE.
     */
    @Override
    protected void onActivityResult(int requestCode, int result, Intent arg2) {
    	if(requestCode == 1){//check if request code is BT code (set earlier)
    		
    		if(result == -1){//bt successfully turned on
    			_isDiscovering = true;
    			_btAdapter.startDiscovery();//bt start discovery
    		}else{	//if bt is still off after the request. show error message.
    			Toast.makeText(getApplicationContext(), "Error! Could not enable Bluetooth!", Toast.LENGTH_SHORT).show();
    		}
    		
    	}
    	// TODO CHECK IF BT IS TURNED ON.
    	super.onActivityResult(requestCode, result, arg2);
    }
}
