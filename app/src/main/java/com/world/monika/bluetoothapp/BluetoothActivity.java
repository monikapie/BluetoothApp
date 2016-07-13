package com.world.monika.bluetoothapp;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import java.util.Calendar;

@TargetApi(18)
public class BluetoothActivity extends AppCompatActivity {
    TextView tv1;
    TextView row;
    ListView lv1;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<String> deviceList = new ArrayList<String>();
    private ArrayList<String> deviceListDetails = new ArrayList<String>();
    private ArrayList<String> macList = new ArrayList<String>();
    private ArrayList<String> macList2 = new ArrayList<String>();
    public final static String tag = "INFO";
    private static final int REQUEST_ENABLE_BT = 1;
    //Scanning for the first time - 0, next scanning - >0
    //to create header and exclude unwanted beacons
    int counter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        tv1 = (TextView) findViewById(R.id.textview1);
        row = (TextView) findViewById(R.id.Row);
        lv1 = (ListView) findViewById(R.id.listview1);
        File traceFile = new File(((Context) this).getExternalFilesDir(null), "TraceFile.txt");

        //Flush file
        if (traceFile.exists()){
            try {
                FileWriter fw = new FileWriter(traceFile, false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Checks whether BLE is supported on the device.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.(API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.)
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks whether Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        Log.i(tag, "Program is started.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Asks if BLE can be enabled.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
        scanLeDevice(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
    }

    @Override
    protected  void onStop(){
        super.onStop();
        macList.clear();
        counter = 0;
    }

    public void showToast(String text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
    }

   public void saveToFile() {
        Log.i(tag, "Saving data to file");
        String macListElement="";
        try {
            File traceFile = new File(((Context) this).getExternalFilesDir(null), "TraceFile.txt");
            if (!traceFile.exists()){
                traceFile.createNewFile();
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(traceFile, true));
            if(counter == 0){
                writer.write("\n" + "Time" + "\t");
                for (String ml:macList) {
                    macListElement = (String) ml;
                    writer.write(macListElement + "-%\t" + macListElement + "-raw\t");
                }
                counter++;
            }
            else {
                String deviceListElement = "";
                DateFormat dateFormat = new SimpleDateFormat("d MMM yyyy, HH:mm");
                String date = dateFormat.format(Calendar.getInstance().getTime());
                writer.write("\n" + date + "\t");
                for (String mL: macList) {
                    for (int j = 0; j < macList2.size(); j++) {
                        if(mL.equals(macList2.get(j))) {
                            deviceListElement = (String) deviceListDetails.get(j);
                            writer.write(deviceListElement + "\t");
                        }
                        else{
                            deviceListElement = getString(R.string.device_disapear) + "\t"
                                    + getString(R.string.device_disapear);
                            writer.write(deviceListElement + "\t");
                        }
                    }
                }
            }
            writer.close();
            // Refresh the data.
            MediaScannerConnection.scanFile((Context) (this),
                    new String[]{traceFile.toString()}, null, null);
        } catch (IOException e) {
            Log.e("FileTestError", "Unable to write to the TraceFile.txt file.");
        }
    }

    //Saves data in every 5th second
    Thread timer = new Thread() {
        public void run () {
            for (;;) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                saveToFile();
            }
        }
    };

    UIUpdater mUIUpdater = new UIUpdater(new Runnable() {
        @Override
        public void run() {
            macList2.clear();
            deviceList.clear();
            deviceListDetails.clear();
        }
    });

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            timer.start();
            mUIUpdater.startUpdates();
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    private String getUuid(byte[] scanRecord) {
        int startByte = 2;
        boolean patternFound = false;
        String uuid="";
        while (startByte <= 5) {
            if (((int) scanRecord[startByte + 2] & 0xff) == 0x02 && //Identifies an iBeacon
                    ((int) scanRecord[startByte + 3] & 0xff) == 0x15) { //Identifies correct data length
                patternFound = true;
                break;
            }
            startByte++;
        }

        if (patternFound) {
            //Convert to hex String
            byte[] uuidBytes = new byte[16];
            System.arraycopy(scanRecord, startByte + 4, uuidBytes, 0, 16);
            String hexString = bytesToHex(uuidBytes);
            uuid = hexString.substring(0, 8) + "-" +
                    hexString.substring(8, 12) + "-" +
                    hexString.substring(12, 16) + "-" +
                    hexString.substring(16, 20) + "-" +
                    hexString.substring(20, 32);
            int major = (scanRecord[startByte + 20] & 0xff) * 0x100 + (scanRecord[startByte + 21] & 0xff);
            int minor = (scanRecord[startByte + 22] & 0xff) * 0x100 + (scanRecord[startByte + 23] & 0xff);
        }
        return uuid;
    }

    static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String mac = device.getAddress();
                            String name = device.getName();
                            //String uuid = getUuid(scanRecord);
                            int strength = (rssi + 100) * 2;
                            if(counter == 0) {
                                if (!macList.contains(mac)) {
                                    macList.add(mac);
                                }
                            }
                            else{
                                if (macList.contains(mac) && !macList2.contains(mac)) {
                                    macList2.add(mac);
                                    deviceList.add(name + " " + mac);
                                    deviceListDetails.add( strength + "\t" + rssi );
                                }
                            }
                            if(deviceList.size()>1){
                                ArrayAdapter adapter = new ArrayAdapter<String>(BluetoothActivity.this, R.layout.row, deviceList){
                                    @Override
                                    public int getCount(){
                                        return deviceList.size();
                                    }
                                    @Override
                                    public String getItem(int position) {
                                        if (deviceList != null && deviceList.size() > position) {
                                            return deviceList.get(position);
                                        } else {
                                            Log.e("AdapterError", "Unable to show adapter");
                                            deviceList.add("Error.");
                                            return deviceList.get(0);
                                        }
                                    }

                                };
                                lv1.setAdapter(adapter);
                                //Listing details about devices after clicking on row
                                lv1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                        showToast(deviceListDetails.get(i));
                                    }
                                });
                            }
                        }
                    });
                }
            };
}
