package com.world.monika.bluetoothapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.ArraySet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class BluetoothActivity extends AppCompatActivity {
    Button b1;
    Button b2;
    Button b3;
    Button b4;
    Button b5;
    TextView tv1;
    TextView tv2;
    TextView row;
    ListView lv1;
    private ArrayList<String> deviceList = new ArrayList<String>();
    private ArrayList<String> deviceListPlus = new ArrayList<String>();
    private ArrayList<String> deviceListAll = new ArrayList<String>();
    public final static String tag = "INFO";
    private static final long SCAN_PERIOD = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        b1 = (Button) findViewById(R.id.button1);
        b2 = (Button) findViewById(R.id.button2);
        b3 = (Button) findViewById(R.id.button3);
        b4 = (Button) findViewById(R.id.button4);
        b5 = (Button) findViewById(R.id.button5);
        tv1 = (TextView) findViewById(R.id.textview1);
        tv2 = (TextView) findViewById(R.id.textview2);
        row = (TextView) findViewById(R.id.Row);
        lv1 = (ListView) findViewById(R.id.listview1);

        Log.e(tag, "Program is started.");
        //Scanning
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanVisibleDevices();
            }
        });
        //Changing visiability
        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                becomeVisible();
            }
        });
        b5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                showHistory();
            }
        });
        //Checking local MAC
        BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
        tv1.setText("Your mac:" + ba.getAddress());
        Log.e(tag, "Your device address:" + ba.getAddress());
        //Starting intent
        if (!ba.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, 1);
        }
    }

    //????
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == Activity.RESULT_OK) {
            Log.e(tag, "Accepted!");
            //BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
        }
    }

    //BroadcastReciever, control over bluetooth
    private final BroadcastReceiver reciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            String action = intent.getAction();
            //What happend when bluetooth is starting
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
            }
            //What happend when bluetooth action finished
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                tv2.setText("");
                //List adapter - CHANGE INTO MAP!!
                lv1.setAdapter(new ArrayAdapter<String>(BluetoothActivity.this, R.layout.row, deviceList));
                //Listing details about devices after clicking on row
                lv1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        showToast(deviceListPlus.get(i));
                    }
                });
                //Saving data to Shared Preferances - REPAIR
                b3.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        Set<String> deviceSet = new HashSet<String>();
                        deviceSet.addAll(deviceListPlus);
                        saveData(deviceSet);
                        showToast("Data saved");
                    }
                });
                //Saving data to File (External)
                b4.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        saveToFile(deviceListAll);
                    }
                });
                //Saving visible devices in lists
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                tv2.setText("Scanning...");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String mac = device.getAddress();
                String name = device.getName();
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                Log.e(tag, "Found device: " + mac + name);
                deviceList.add(name);
                deviceListPlus.add(mac + "\n" + (rssi + 100) * 2 + "%" + "\n" + rssi + "dB");
                deviceListAll.add(name + "\n" + mac + "\n" + (rssi + 100) * 2 + "%" + "\n" + rssi + "dB");
            }
        }

    };

    //Unregistering reciever
    @Override
    public void onPause() {
        unregisterReceiver(reciever);
        super.onPause();
    }

    //Changing visiability
    public void becomeVisible() {
        Log.e(tag, "I am visible");
        Intent beVisible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        beVisible.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(beVisible);
    }

    //Scanning
    public void scanVisibleDevices() {
        deviceList.clear();
        deviceListPlus.clear();
        deviceListAll.clear();
        Log.e(tag, "I am looking for visible devices");
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(reciever, filter);
        BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
        ba.startDiscovery();
    }

    //Showing toast
    public void showToast(String text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
    }

    //Saving data in Shared Preferences
    public void saveData(Set details) {
        Log.e(tag, "Saving data to SP");
        SharedPreferences bluetoothPref = getApplicationContext().getSharedPreferences("BluePref", MODE_PRIVATE);
        SharedPreferences.Editor editor = bluetoothPref.edit();
        String k = "dg";
        editor.putStringSet(k, details);
        editor.commit();
    }

    //Saving to file
    public void saveToFile(ArrayList listOfDevices) {
        Log.e(tag, "Saving data to file");
        String SLOD = "";
        DateFormat df = new SimpleDateFormat("d MMM yyyy, HH:mm");
        String date = df.format(Calendar.getInstance().getTime());
        try {
            // Creates a trace file in the primary external storage space of the
            // current application.
            // If the file does not exists, it is created.
            File traceFile = new File(((Context) this).getExternalFilesDir(null), "TraceFile.txt");
            if (!traceFile.exists())
                traceFile.createNewFile();
            // Adds a line to the trace file
            BufferedWriter writer = new BufferedWriter(new FileWriter(traceFile, true /*append*/));
            writer.write(date);
            writer.write("---------------------------------------------------------------------\n");
            for (int i = 0; i < listOfDevices.size() - 1; i++) {
                SLOD = (String) listOfDevices.get(i);
                writer.write(SLOD);
                writer.write("\n");
            }
            writer.close();
            // Refresh the data so it can seen when the device is plugged in a
            // computer. You may have to unplug and replug the device to see the
            // latest changes. This is not necessary if the user should not modify
            // the files.
            MediaScannerConnection.scanFile((Context) (this),
                    new String[]{traceFile.toString()},
                    null,
                    null);
        } catch (IOException e) {
            Log.e("FileTestError", "Unable to write to the TraceFile.txt file.");
        }
    }

//    public void showHistory() {
//        try {
//
//        } catch (Exception e) {
//            Log.e(tag, "File with this name doesn't exist.");
//            showToast("File with this name doesn't exist.");
//        }
//
//    }
}
