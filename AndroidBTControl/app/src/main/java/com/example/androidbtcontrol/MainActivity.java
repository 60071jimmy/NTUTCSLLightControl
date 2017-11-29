/*
Android Example to connect to and communicate with Bluetooth
In this exercise, the target is a Arduino Due + HC-06 (Bluetooth Module)

Ref:
- Make BlueTooth connection between Android devices
http://android-er.blogspot.com/2014/12/make-bluetooth-connection-between.html
- Bluetooth communication between Android devices
http://android-er.blogspot.com/2014/12/bluetooth-communication-between-android.html
- Making TextView scrollable on Android
https://stackoverflow.com/questions/1748977/making-textview-scrollable-on-android

 */
package com.example.androidbtcontrol;

import android.app.Activity;															//	引入android.app.Activity函式庫
import android.bluetooth.BluetoothAdapter;												//	引入android.bluetooth.BluetoothAdapter函式庫
import android.bluetooth.BluetoothDevice;												//	引入android.bluetooth.BluetoothDevice函式庫
import android.bluetooth.BluetoothSocket;												//	引入android.bluetooth.BluetoothSocket函式庫
import android.content.Intent;															//	引入android.content.Intent函式庫
import android.content.pm.PackageManager;												//	引入android.content.pm.PackageManager函式庫
import android.os.Bundle;																//	引入android.os.Bundle函式庫
import android.support.v7.app.ActionBarActivity;										//	引入android.support.v7.app.ActionBarActivity函式庫
import android.text.method.ScrollingMovementMethod;										//	引入android.text.method.ScrollingMovementMethod函式庫
import android.view.View;																//	引入android.view.View函式庫
import android.widget.AdapterView;														//	引入android.widget.AdapterView函式庫
import android.widget.ArrayAdapter;														//	引入android.widget.ArrayAdapter函式庫
import android.widget.Button;															//	引入android.widget.Button函式庫
import android.widget.EditText;															//	引入android.widget.EditText函式庫
import android.widget.LinearLayout;														//	引入android.widget.LinearLayout函式庫
import android.widget.ListView;															//	引入android.widget.ListView函式庫
import android.widget.TextView;															//	引入android.widget.TextView函式庫
import android.widget.Toast;															//	引入android.widget.Toast函式庫

import java.io.IOException;																//	引入java.io.IOException函式庫
import java.io.InputStream;																//	引入java.io.InputStream函式庫
import java.io.OutputStream;															//	引入java.io.OutputStream函式庫
import java.util.ArrayList;																//	引入java.util.ArrayList函式庫
import java.util.Set;																	//	引入java.util.Set函式庫
import java.util.UUID;																	//	引入java.util.UUID函式庫

public class MainActivity extends ActionBarActivity										//	MainActivity類別
{																						//	進入MainActivity類別
	private static final int REQUEST_ENABLE_BT = 1;										//	宣告REQUEST_ENABLE_BT變數

	BluetoothAdapter bluetoothAdapter;													//	建立BluetoothAdapter物件bluetoothAdapter

	ArrayList<BluetoothDevice> pairedDeviceArrayList;									//	宣告pairedDeviceArrayList記錄已配對的裝置

	TextView textInfo, textStatus, textByteCnt;											//	建立textInfo、textStatus、textByteCnt為TextView物件
	ListView listViewPairedDevice;														//	建立listViewPairedDevice為ListView物件
	LinearLayout inputPane;																//	建立inputPane為LinearLayout物件
	EditText inputField;																//	建立inputField為EditText物件
	Button btnSend, btnClear;															//	建立btnSend、btnClear為Button物件

	ArrayAdapter<BluetoothDevice> pairedDeviceAdapter;									//	建立pairedDeviceAdapter為BluetoothDevice型態之ArrayAdapter
	private UUID myUUID;																//	建立myUUID為UUID私有物件
	private final String UUID_STRING_WELL_KNOWN_SPP =									//	建立UUID_STRING_WELL_KNOWN_SPP字串，設定內容為"00001101-0000-1000-8000-00805F9B34FB"
		"00001101-0000-1000-8000-00805F9B34FB";

	ThreadConnectBTdevice myThreadConnectBTdevice;										//	建立myThreadConnectBTdevice為ThreadConnectBTdevice物件
	ThreadConnected myThreadConnected;													//	建立myThreadConnected為ThreadConnected物件

	@Override
	protected void onCreate(Bundle savedInstanceState) {								//	APP開啟
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);											//	設定activity_main

		setTitle("MIT: City Science Lab@ Taipei Tech");
		textInfo = (TextView)findViewById(R.id.info);									//	設定textInfo連結至Layout中的info物件
		textStatus = (TextView)findViewById(R.id.status);								//	設定textStatus連結至Layout中的status物件
		textByteCnt = (TextView)findViewById(R.id.textbyteCnt);							//	設定textByteCnt連結至Layout中的textbyteCnt物件
		listViewPairedDevice = (ListView)findViewById(R.id.pairedlist);					//	設定listViewPairedDevice連結至Layout中的pairedlist物件

		inputPane = (LinearLayout)findViewById(R.id.inputpane);							//	設定inputPane連結至Layout中的inputpane物件
		inputField = (EditText)findViewById(R.id.input);								//	設定inputField連結至Layout中的input物件
		btnSend = (Button)findViewById(R.id.send);										//	設定btnSend連結至Layout中的send物件
		btnSend.setOnClickListener(new View.OnClickListener(){							//	處理send按鈕按下事件

			@Override
			public void onClick(View v) 												//	建立onClick方法
			{																			//	進入onClick方法
				if(myThreadConnected!=null)
				{
					byte[] bytesToSend = inputField.getText().toString().getBytes();	//	
					myThreadConnected.write(bytesToSend);
					byte[] NewLine = "\n".getBytes();
					myThreadConnected.write(NewLine);
				}
			}});

		btnClear = (Button)findViewById(R.id.clear);
		btnClear.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				textStatus.setText("");
				textByteCnt.setText("");
			}
		});

		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)){
			Toast.makeText(this,
					"FEATURE_BLUETOOTH NOT support",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		//using the well-known SPP UUID
		myUUID = UUID.fromString(UUID_STRING_WELL_KNOWN_SPP);

		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter == null) {
			Toast.makeText(this,
					"Bluetooth is not supported on this hardware platform",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		String stInfo = bluetoothAdapter.getName() + "\n" +
				bluetoothAdapter.getAddress();
		textInfo.setText(stInfo);
	}

	@Override
	protected void onStart() {
		super.onStart();

		//Turn ON BlueTooth if it is OFF
		if (!bluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		}

		setup();
	}

	private void setup() {
		Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
		if (pairedDevices.size() > 0) {
			pairedDeviceArrayList = new ArrayList<BluetoothDevice>();

			for (BluetoothDevice device : pairedDevices) {
				pairedDeviceArrayList.add(device);
			}

			pairedDeviceAdapter = new ArrayAdapter<BluetoothDevice>(this,
					android.R.layout.simple_list_item_1, pairedDeviceArrayList);
			listViewPairedDevice.setAdapter(pairedDeviceAdapter);

			listViewPairedDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view,
										int position, long id) {
					BluetoothDevice device =
							(BluetoothDevice) parent.getItemAtPosition(position);
					Toast.makeText(MainActivity.this,
							"Name: " + device.getName() + "\n"
									+ "Address: " + device.getAddress() + "\n"
									+ "BondState: " + device.getBondState() + "\n"
									+ "BluetoothClass: " + device.getBluetoothClass() + "\n"
									+ "Class: " + device.getClass(),
							Toast.LENGTH_LONG).show();

					textStatus.setText("start ThreadConnectBTdevice");
					myThreadConnectBTdevice = new ThreadConnectBTdevice(device);
					myThreadConnectBTdevice.start();
				}
			});
		}
	}

	@Override
	protected void onDestroy()																	//	onDestroy程式
	{																							//	進入onDestroy程式
		super.onDestroy();																		//	

		if(myThreadConnectBTdevice!=null)														//	若myThreadConnectBTdevice不為null，代表藍芽連線尚未結束
		{																						//	進入if敘述
			myThreadConnectBTdevice.cancel();													//	將myThreadConnectBTdevice結束
		}																						//	結束if敘述
	}																							//	結束onDestroy程式

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 				//	onActivityResult方法
	{																							//	進入onActivityResult方法
		if(requestCode == REQUEST_ENABLE_BT){
			if(resultCode == Activity.RESULT_OK){
				setup();
			}else{
				Toast.makeText(this,
						"BlueTooth NOT enabled",
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	//Called in ThreadConnectBTdevice once connect successed
	//to start ThreadConnected
	private void startThreadConnected(BluetoothSocket socket){

		myThreadConnected = new ThreadConnected(socket);
		myThreadConnected.start();
		if(myThreadConnected!=null) {
			byte[] bytesToSend = "powerof".toString().getBytes();    //
			myThreadConnected.write(bytesToSend);
			byte[] NewLine = "\n".getBytes();
			myThreadConnected.write(NewLine);
		}
	}

	/*
	ThreadConnectBTdevice:
	Background Thread to handle BlueTooth connecting
	*/
	private class ThreadConnectBTdevice extends Thread {

		private BluetoothSocket bluetoothSocket = null;
		private final BluetoothDevice bluetoothDevice;


		private ThreadConnectBTdevice(BluetoothDevice device) {
			bluetoothDevice = device;

			try {
				bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID);
				textStatus.setText("bluetoothSocket: \n" + bluetoothSocket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			boolean success = false;
			try {
				bluetoothSocket.connect();
				success = true;
			} catch (IOException e) {
				e.printStackTrace();

				final String eMessage = e.getMessage();
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						textStatus.setText("something wrong bluetoothSocket.connect(): \n" + eMessage);
					}
				});

				try {
					bluetoothSocket.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

			if(success){
				//connect successful
				final String msgconnected = "connect successful:\n"
						+ "BluetoothSocket: " + bluetoothSocket + "\n"
						+ "BluetoothDevice: " + bluetoothDevice;

				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						textStatus.setText("");
						textByteCnt.setText("");
						Toast.makeText(MainActivity.this, msgconnected, Toast.LENGTH_LONG).show();

						listViewPairedDevice.setVisibility(View.GONE);
						inputPane.setVisibility(View.VISIBLE);
					}
				});

				startThreadConnected(bluetoothSocket);

			}else{
				//fail
			}
		}

		public void cancel() {

			Toast.makeText(getApplicationContext(),
					"close bluetoothSocket",
					Toast.LENGTH_LONG).show();

			try {
				bluetoothSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	/*
	ThreadConnected:
	Background Thread to handle Bluetooth data communication
	after connected
	 */
	private class ThreadConnected extends Thread												//	ThreadConnected類別
	{																							//	進入ThreadConnected類別
		private final BluetoothSocket connectedBluetoothSocket;									//	建立connectedBluetoothSocket為BluetoothSocket私有物件
		private final InputStream connectedInputStream;											//	建立connectedInputStream為InputStream私有物件
		private final OutputStream connectedOutputStream;

		public ThreadConnected(BluetoothSocket socket) {
			connectedBluetoothSocket = socket;
			InputStream in = null;
			OutputStream out = null;

			try {
				in = socket.getInputStream();
				out = socket.getOutputStream();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			connectedInputStream = in;
			connectedOutputStream = out;
		}

		@Override
		public void run() {
			byte[] buffer = new byte[1024];
			int bytes;																			//	宣告bytes變數，用以記錄接收位元組數目
			while (true) {
				try {
					bytes = connectedInputStream.read(buffer);									//	讀取接收資料
					final String strReceived = new String(buffer, 0, bytes);					//	將buffer所收到資料轉換為字串
					final String strByteCnt = String.valueOf(bytes) + " bytes received.\n";		//	生成strByteCnt字串記錄收到資料位元組數量
					
					char[] charArray = strReceived.toCharArray();								//	將buffer所收到資料字串轉換為字元陣列
					String strReceivedAddValue = "";											//	宣告strReceivedAddValue字串並初始化為空字串
					for (char charReceived : charArray)											//	依序取出字串中字元
					{																			//	進入for迴圈
						strReceivedAddValue = "[" + charReceived + "] (" + (byte)charReceived + "),";
						//	以strReceivedAddValue字串記錄接收字元及其ASCII編碼(ASCII編碼顯示於括號中)，字元間以逗號隔開，如：[A] (65),[B] (66)
					}																			//	結束for迴圈
					final String showStrReceived = strReceivedAddValue;							//	將strReceivedAddValue傳遞給showStrReceived，用於顯示
					runOnUiThread(new Runnable(){
						@Override
						public void run() {
							if(textStatus.getLineCount() > 100)									//	若textStatus文字方塊行數大於100行
							{																	//	進入if敘述
								textStatus.setText("");											//	清除文字方塊內容
							}																	//	結束if敘述
							if(textByteCnt.getLineCount() > 100)								//	若textByteCnt文字方塊行數大於100行
							{																	//	進入if敘述
								textByteCnt.setText("");										//	清除文字方塊內容
							}																	//	結束if敘述
							textStatus.append(showStrReceived);									//	顯示接收字串
							textStatus.setMovementMethod(ScrollingMovementMethod.getInstance());//	捲動至最底部
							textByteCnt.append(strByteCnt);										//	更新textByteCnt，顯示最新收到資料位元組數量
							textByteCnt.setMovementMethod(ScrollingMovementMethod.getInstance());
							//	捲動至最底部
							
						}});

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();

					final String msgConnectionLost = "Connection lost:\n"
							+ e.getMessage();
					runOnUiThread(new Runnable(){

						@Override
						public void run() {
							textStatus.setText(msgConnectionLost);
						}});
				}
			}
		}

		public void write(byte[] buffer) {
			try {
				connectedOutputStream.write(buffer);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void cancel() {
			try {
				connectedBluetoothSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}																							//	結束ThreadConnected類別


	public void scrollToTop(View v) {
		if(myThreadConnected!=null) {
			byte[] bytesToSend = "poweron".toString().getBytes();    //
			myThreadConnected.write(bytesToSend);
			byte[] NewLine = "\n".getBytes();
			myThreadConnected.write(NewLine);
		}
	}
}
