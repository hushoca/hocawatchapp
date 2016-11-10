package com.hhoca.hocawatch;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Action;
import android.widget.Toast;

public class BluetoothService extends Service{

	//INIT VARIABLES
	IntentFilter 		_intentFilter 			= new IntentFilter();
	static boolean 		isConnected 			= false;
	static boolean 		isRunning				= false;
	static boolean		isGPSUpdateScheduled 	= false;
	static boolean		isTimeUpdateScheduled	= false;
	BluetoothDevice 	device 					= null;
	String 				stringUUID				= "00001101-0000-1000-8000-00805F9B34FB";
	UUID				uuid					= UUID.fromString(stringUUID);
	BluetoothSocket 	_btSocket				= null;
	InputStream			_inputStream			= null;
	OutputStream		_outputStream			= null;
	
	//INIT EVENTS LIKE BROADCASTRECEIVER
	BroadcastReceiver 	_bluetoothBroadcastReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			String action = arg1.getAction();
			//DETECT IF DEVICE IS DISCONNECTED
			if(action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)){
				//kill the service.
				stopSelf();
			}
		}
		
	};
	
	/***
	 * IS CALLED TO MAKE THE SERVICE A FOREGROUND SERVICE. MEANING THAT THE USER WILL SEE THIS SERVICE
	 * RUNNING THEREFORE ANDROID WILL NEVER SHUTDOWN THIS SERVICE BECAUSE IT CAN EFFECT THE USER PERFORMANCE
	 * EVEN IF MORE MEMORY IS NEEDED. (to make a service foreground service an notification must be built
	 * and an id must be supplied into startForeground service function)
	 */
	private void startForegroundService(){
		
		//notification sound.
		Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		
		//setup intent which will shutdown the service when called
		Intent intent = new Intent(getApplicationContext(), BluetoothService.class);
		intent.setAction(Intent.ACTION_SHUTDOWN);
		PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, intent, 0);
		
		//get the builder and start stacking title,sound,icon commands...
		NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
		builder.setContentTitle("Hoca Watch")
		.setSound(notificationSound)
		.setSmallIcon(R.drawable.hoca_notification_icon)
		.setContentText("Connected to "+device.getName())
		//this part will add a button with an X icon and disconnect text. if button is clicked the
		//pending intent which setup earlier will be called and it will kill the service
		//which will end up disconnecting the service from the device.
		.addAction(new Action(R.drawable.hoca_notification_disconnect_button, "Disconnect", pendingIntent));
		
		//create the notification using the builder and start the foreground service
		//using the notification and id 1.
		Notification notification = builder.build();
		startForeground(1, notification);
	}
	
	/***
	 * TOAST MESSAGE USING HANDLER (THREAD SAFE.)
	 * @param msg
	 */
	private void ToastWithHandler(final String msg){
		Handler handler = new Handler(getMainLooper());
		handler.post(new Runnable() {
			public void run() {
				Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	/***
	 * USED FOR WRITING TO THE BT DEVICE (TO THE WATCH)
	 * @param msg = msg which will be sent to the watch
	 */
	private void writeToBT(String msg){
		//TO PROTECT FROM PROGRAMMER ERROR
		//(even though i am a perfect programmer with no mistakes...JK)
		if(_outputStream != null){
			//for each char in chararray do the loop.
			for(char chr:msg.toCharArray()){
				try {
					_outputStream.write((byte) chr);	//send the char to the bt
				} catch (IOException e){				//if it fails...well exception happens...
					//TODO maybe implement a notification which will popup when a packet goes
					//missing and this throws exception
				}
			}
			
		}
	}
	
	/***
	 * THIS IS THE FUNCTION THAT HANDLES THE MESSAGES COMING FROM THE WATCH
	 * TURNS THEM INTO COMMANDS THEN DOES WHATS NECESSARY
	 * @param msg
	 */
	private void handleMessage(String msg){
		if(msg.contains(" ")){
			ArrayList<String> args = new ArrayList<String>();	//create a new list
			//split arguments by " ". then add each argument into the list
			for(String s:msg.split(" ")) args.add(s);			
			String command = args.get(0);	//get the first item on the list. which is the command
			args.remove(0);					//remove the command from the argument list
			
			//COMMANDS WITH ARGUMENTS GO HERE

		}else{
			
			
	}
	
	/***
	 * WILL HANDLE THE MESSAGES COMING FROM THE INPUT STRINGS AND TURN THEM INTO MESSAGES
	 * AND COMMANDS. MESSAGES WILL BE PASSED TO THE handleMessages() FUNCTION.
	 */
	Thread _inputStreamListener = new Thread(new Runnable(){
		
		@Override
		public void run() {
			//setup string buffer which will hold the message until \n or  is received
			String msgBuffer = "";
			
			//SETUP INFINITE LOOP
			while(true){
				
				char chr = 0;	//init char
				
				try {
					chr = (char) _inputStream.read();//read the incoming byte (char)
				} catch (IOException e){
					stopSelf();//kill service if exception caught
				}
				
				if(chr == 13 || chr == 10){
					ToastWithHandler(msgBuffer);	//DEBUG for debug process
					handleMessage(msgBuffer);
					msgBuffer = "";	//clear the msgbuffer so does not keep previous line content
				}else{
					msgBuffer += chr;//add character to the buffer to make a string
				}
				
			}
			
		}
		
	});
	
	/***
	 * USED FOR SETTING UP CONNECTION.
	 * @return true if connected/ false if connection failed
	 */
	private boolean attemptConnection(){
		
		//TRY TO CREATE SOCKET IF FAILED RETURN FALSE
		try {
			_btSocket = device.createRfcommSocketToServiceRecord(uuid);
		} catch (IOException e) {
			return false;
		}
		
		//TRY TO CONNECT TO THE SOCKET IF FAILED RETURN FALSE
		try {
			_btSocket.connect();
		} catch (IOException e) {
			return false;
		}
		
		//TRY TO GET THE INPUTSTREAM IF FAILED RETURN FALSE
		try {
			_inputStream = _btSocket.getInputStream();
		} catch (IOException e) {
			return false;
		}
		
		//TRY TO GET THE OUTPUTSTREAM IF FAILED RETURN FALSE
		try {
			_outputStream = _btSocket.getOutputStream();
		} catch (IOException e) {
			return false;
		}
				
		/*at this point device must be connected perfectly with socket setup,
		 *input and output streams must be correctly setup as well. So listener
		 *the listener thread can be started here (will listen to the bytes coming
		 *from the watch).
		 */

		_inputStreamListener.start();
		
		return true;
	}

	
	/***
	 * CALLED WHEN THE SERVICE IS FIRST STARTED.
	 * @param intent HAS THE EXTRAS WHICH IN THIS CASE IS BT_DEVICE EXTRA
	 * WHICH CONTAINS THE BT DEVICE WE ARE CONNECTING TO.
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		if(intent.hasExtra("BT_DEVICE")){	//check to see if has the BT_DEVICE.
			
			isRunning = true;				//if so set running true
			device = intent.getParcelableExtra("BT_DEVICE");	//get the bt device
			
			if(attemptConnection()){	//attempt to connect to device if successful:
				isConnected = true;
				writeToBT("CONNECTED\n");
				
				writeToBT("GPS_UPDATE_INTERVAL "+"insertvariablehere"+"\n");//IF 0 THEN SELECTED BY WATCH/ IF NOT FORCE DATA FROM PHONE
				writeToBT("TIME_SYNC_INTERVAL "+"insertvariablehere"+"\n");//BETWEEN 30 MINS AND 5 HOURS
				writeToBT("SILENT_MODE "+"insertvariablehere"+"\n");// 1 means true 0 means false
				
				startForegroundService();	//turn the service into foregroundService
			}else{			//if failed to connect to the device
				Toast.makeText(getApplicationContext(), "Could not connect to "
				+device.getName(), Toast.LENGTH_SHORT).show();
				stopSelf();	//kill the service
			}
			
			//REGISTER BROADCASTER WHICH WILL DETECT WHEN THE DEVICE IS DC'ed
			_intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);	//when bt is disconnected from device
			registerReceiver(_bluetoothBroadcastReceiver, _intentFilter);		//register broadcastreceiver
		
		}else{
			stopSelf();
		}
		
		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	public void onDestroy() {
		
		//get rid of the register so it does not leak.
		unregisterReceiver(_bluetoothBroadcastReceiver);
		
		//if connected and gets killed : show "Connection Closed" and make it long delay for dramatic effect.
		if(isConnected){
			isConnected = false; //extra precaution
			
			writeToBT("CONNECTION_CLOSE\n");//to let the watch know that connection is closed on purpose
			
			try {
				_btSocket.close();	//kill the connection to the bt device
			} catch (Exception e){}
			
			Toast.makeText(getApplicationContext(), "Connection Closed.", Toast.LENGTH_LONG).show();
		}
		
		isRunning = false;
		super.onDestroy();
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

}
