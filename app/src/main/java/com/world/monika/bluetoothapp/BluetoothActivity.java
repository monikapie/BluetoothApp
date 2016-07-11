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
    int summand = 0;
    TextView tv1;
    TextView tv2;
    TextView tv4;
    TextView row;
    ListView lv1;
    private ArrayList<String> deviceList = new ArrayList<String>();
    private ArrayList<String> deviceListPlus = new ArrayList<String>();
    private ArrayList<String> deviceListAll = new ArrayList<String>();
    private ArrayList<String> macList = new ArrayList<String>();
    public final static String tag = "INFO";
    private BluetoothAdapter mBluetoothAdapter;

    private static final int REQUEST_ENABLE_BT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        tv1 = (TextView) findViewById(R.id.textview1);
        tv2 = (TextView) findViewById(R.id.textview2);
        tv4 = (TextView) findViewById(R.id.textview4);
        row = (TextView) findViewById(R.id.Row);
        lv1 = (ListView) findViewById(R.id.listview1);


        tv4.setText("Visible devices with BLE");

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
        // If Bluetooth is not currently enabled, ask if BLE can be enabled.
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

    public void showToast(String text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
    }

    //Saving to file
    public void saveToFile(ArrayList listOfDevices) {
        Log.i(tag, "Saving data to file");
        String SListOfDevices = "";
        DateFormat dataFormat = new SimpleDateFormat("d MMM yyyy, HH:mm");
        String date = dataFormat.format(Calendar.getInstance().getTime());
        try {
            // Saving to primary external storage space of the current application.
            File traceFile = new File(((Context) this).getExternalFilesDir(null), "TraceFile.txt");
            if (!traceFile.exists()){
                traceFile.createNewFile();
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(traceFile, true));
            writer.write("\n"+date);
            writer.write("  ");
            for (int i = 0; i < listOfDevices.size() - 1; i++) {
                SListOfDevices = (String) listOfDevices.get(i);
                writer.write(SListOfDevices);
            }
            writer.close();
            // Refresh the data.
            MediaScannerConnection.scanFile((Context) (this),
                    new String[]{traceFile.toString()},
                    null,
                    null);
        } catch (IOException e) {
            Log.e("FileTestError", "Unable to write to the TraceFile.txt file.");
        }
    }

    Thread timer = new Thread() {
        public void run () {
            for (;;) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                saveToFile(deviceListAll);
            }
        }
    };

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            deviceList.clear();
            deviceListAll.clear();
            macList.clear();
            macList.add(" ");
            timer.start();
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            Log.i(tag, "Scanning stopped and details saved to file");
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

            //Here is your UUID
            uuid = hexString.substring(0, 8) + "-" +
                    hexString.substring(8, 12) + "-" +
                    hexString.substring(12, 16) + "-" +
                    hexString.substring(16, 20) + "-" +
                    hexString.substring(20, 32);

            //Here is your Major value
            int major = (scanRecord[startByte + 20] & 0xff) * 0x100 + (scanRecord[startByte + 21] & 0xff);

            //Here is your Minor value
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
                            //Log.i(tag,"Scanning..");
                            String mac = device.getAddress();
                            String name = device.getName();
                            String uuid = getUuid(scanRecord);
                            int strength =  (rssi+100)*2;

                            for(int i = 0; i <= macList.size(); i++){
                                if(!macList.contains(mac)){
                                    macList.add(mac);
                                    deviceList.add(name+" "+mac);
                                    deviceListAll.add(name+" "+mac+" "+rssi+" "+strength+" "+uuid);
                                    deviceListPlus.add(rssi+" "+strength+" "+uuid);
                                }
                            }
                            lv1.setAdapter(new ArrayAdapter<String>(BluetoothActivity.this, R.layout.row, deviceList));
                            //Listing details about devices after clicking on row
                            lv1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                    showToast(deviceListPlus.get(i));
                                }
                            });
                        }
                    });
                }
            };
}
