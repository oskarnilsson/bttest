package com.example.jaosn.bttest;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import java.util.ArrayList;

public class BtScanActivity extends AppCompatActivity {

    private static int REQUEST_ENABLE_BT = 2;
    private BluetoothAdapter mBluetoothAdapter;
    public MyReceiver bReceiver;
    private String deviceName;
    private String deviceAddress;
    private ArrayList<BluetoothDevice> mBtDevices;
    private ListView lv;
    private ArrayAdapter<BluetoothDevice> arrayAdapter;
    /*/Stuff to try to add device name instead of address NEW
    private ArrayList<String> deviceNameList;
    private ArrayAdapter<String> nameAdapter;
    //END */

    private BluetoothLeService mBluetoothLeService;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder)service).getService();
            if (mBluetoothLeService != null) {
                // Automatically connects to the device upon successful start-up initialization.
                Toast.makeText(getApplicationContext(), "Service initialized", Toast.LENGTH_SHORT).show();
                Log.d("BluetoothLeService","Service initialized");
            }
            if (!mBluetoothLeService.initialize()) {
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt_scan);
        lv = findViewById(R.id.listview);
        setTitle("Bluetooth ECG");

        mBtDevices = new ArrayList<>(); //Initialize arrayList to store found devices
        //deviceNameList = new ArrayList<>(); //NEW

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        //Initialize BT adapter
        //mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothManager bm = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bm.getAdapter();

        // Phone does not support Bluetooth so let the user know and exit.
        if (mBluetoothAdapter == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Not compatible")
                    .setMessage("Your phone does not support Bluetooth")
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
        // Is Bluetooth enabled? If not, enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            mBluetoothAdapter.enable();
        }
        // Initialize broadcast receiver
        bReceiver = new MyReceiver();

        final IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);

        final Button button = findViewById(R.id.scan);
        button.setText(R.string.scan);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBluetoothAdapter.isDiscovering()) {
                    button.setText(R.string.stop);
                    registerReceiver(bReceiver, filter);
                    mBtDevices.clear(); //Clear device list before new scan.
                    arrayAdapter.notifyDataSetChanged();
                    mBluetoothAdapter.startDiscovery();
                    Log.d("startDiscovery", "Discovery started: " + String.valueOf(mBluetoothAdapter.isDiscovering()));
                    Toast.makeText(getApplicationContext(), "Searching", Toast.LENGTH_SHORT).show();
                } else {
                    button.setText(R.string.scan);
                    unregisterReceiver(bReceiver);
                    mBluetoothAdapter.cancelDiscovery();
                    mBtDevices.clear();
                    arrayAdapter.notifyDataSetChanged();
                    Toast.makeText(getApplicationContext(),"Stopped searching", Toast.LENGTH_SHORT).show();
                }
            }
        });

        arrayAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                mBtDevices);
        //NEW, try to add name instead
        //nameAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceNameList);

        lv.setAdapter(arrayAdapter);
        //lv.setAdapter(nameAdapter); Try to add name instead
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getApplicationContext(),
                        "Click ListItem Number " + position, Toast.LENGTH_SHORT).show();
                BluetoothDevice device = mBtDevices.get(position);

                Intent intent = new Intent(getApplicationContext(), ConnectionActivity.class);
                intent.putExtra("com.example.jaosn.bttest.BtDevice", device);
                startActivity(intent);
                unregisterReceiver(bReceiver);
                mBluetoothAdapter.cancelDiscovery();
                mBluetoothLeService.connect(mBtDevices.get(position).getAddress());
            }
        });

    } // onCreate()


    public class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //Log.d("intent.getAction()", "got action from intent");
            if (BluetoothDevice.ACTION_FOUND.equals(action)) { //Unnecessary if statement ?
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                deviceName = device.getName(); //Pass these on in an intent
                deviceAddress = device.getAddress();
                mBtDevices.add(device); //Adding the found device to the array adapter
                arrayAdapter.notifyDataSetChanged();

                /*
                deviceNameList.add(deviceName);
                nameAdapter.notifyDataSetChanged(); */ //Try to add name instead
            }
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        //unregisterReceiver(bReceiver);
        //mBluetoothAdapter.disable();
        mBtDevices.clear();
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        mBluetoothAdapter.disable();
        mBtDevices.clear();
    }
    @Override
    protected void onResume() {
        super.onResume();
        final IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bReceiver, filter);
        mBtDevices.clear();
    }

} //Class close
