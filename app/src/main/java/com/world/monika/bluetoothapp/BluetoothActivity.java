package com.world.monika.bluetoothapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class BluetoothActivity extends AppCompatActivity {
    Button b1;
    Button b2;
    TextView tv1;
    ListView lv1;
    private ArrayList<String> deviceList = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        b1=(Button)findViewById(R.id.button1);
        b2=(Button)findViewById(R.id.button2);
        tv1=(TextView)findViewById(R.id.textview1);
        lv1=(ListView)findViewById(R.id.listview1);

        Log.d("INFO", "Program is started.");
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showVisibleDevices();
            }
        });
        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                beVisible();
            }
        });

        BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
        tv1.setText("Your mac:"+ba.getAddress());
        Log.d("INFO", "Your device address:"+ba.getAddress());
        if(!ba.isEnabled()){
            Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent,1);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
        if(resultCode== Activity.RESULT_OK){
            Log.d("INFO","Accepted!");
            BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
        }
    }
    private final BroadcastReceiver reciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String status="";
                if(device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    status = "nie sparowane";
                }else{
                    status="sparowane";
                }
                deviceList.add(device.getName()+"\n"+device.getAddress());
                Log.d("INFO", "Znaleziono urządzenie: "+device.getName()+" - "+device.getAddress()+" - "+status);
                lv1.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, deviceList));
            }
        }
    };

    public void beVisible(){
        Log.d("INFO", "I am visible");
        Intent beVisible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        beVisible.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(beVisible);
    }

    public void showVisibleDevices(){
        Log.d("INFO", "I am looking for visible devices");
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(reciever, filter);
        BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
        ba.startDiscovery();
    }

}
