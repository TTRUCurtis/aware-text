/**
 * Created by: Aayush Chadha
 * Last Updated: 26th October 2017
 * Adapted from:
 * 1) https://github.com/denzilferreira/aware-plugin-template
 * 2) https://github.com/denzilferreira/com.aware.plugin.device_usage
 * 3) https://github.com/denzilferreira/com.aware.plugin.fitbit
 */
package com.aware.plugin.sensortag;


import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class DevicePicker extends AppCompatActivity {

    private HashSet<BluetoothDevice> mDeviceList = new HashSet<>();
    private BluetoothAdapter mBluetoothAdapter;

    private int REQUEST_ENABLE_BT = 1;

    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000; //10 seconds searching for devices

    private ScanSettings settings;
    private List<ScanFilter> filters;

    private BluetoothGatt mGatt;
    private BluetoothLeScanner mLEScanner;
    private Measurement data;

    private LinearLayout devicePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth Low Energy not supported on this device :(", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mHandler = new Handler();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        setContentView(R.layout.activity_device_picker);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        devicePicker = findViewById(R.id.device_picker);

        Button resetDevices = findViewById(R.id.clear_devices);
        resetDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDeviceList.clear();
                getContentResolver().delete(Provider.SensorTag_Devices.CONTENT_URI, null, null); //wip table clean
                recreate(); //restart activity
            }
        });

        Button selectDevices = findViewById(R.id.select_devices);
        selectDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for(BluetoothDevice e : mDeviceList) {
                    connectToDevice(e);
                }
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBluetoothIntent = new Intent((BluetoothAdapter.ACTION_REQUEST_ENABLE));
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                filters = new ArrayList<>();
            }
            scanLeDevice(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void scanLeDevice(boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT < 21) {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    } else {
                        mLEScanner.stopScan(mScanCallback);
                    }
                }
            }, SCAN_PERIOD);

            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                mLEScanner.startScan(filters, settings, mScanCallback);
            }
        } else {
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {
                mLEScanner.stopScan(mScanCallback);
            }
        }
    }

    private final ScanCallback mScanCallback = new ScanCallback() {
        @TargetApi(21)
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            if (result != null && result.getDevice() != null) {
                BluetoothDevice bleDevice = result.getDevice();
                if (bleDevice.getName() != null && bleDevice.getType() == BluetoothDevice.DEVICE_TYPE_LE && !mDeviceList.contains(bleDevice) && (bleDevice.getName().contains("SensorTag"))) {
                    mDeviceList.add(bleDevice);
                    addToView(bleDevice);
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                Log.i("ScanResult - Results", result.toString());
            }
        }

        public void onScanFailed(int errorCode) {
            Log.e("Scan failed", "Error Code: " + errorCode);
        }
    };

    private void addToView(final BluetoothDevice bluetoothDevice) {
        CheckBox smartag = new CheckBox(this);
        smartag.setTag(bluetoothDevice.getAddress());
        smartag.setText(bluetoothDevice.getName());
        smartag.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mDeviceList.add(bluetoothDevice);
                    try {
                        JSONObject jsonTag = new JSONObject();
                        jsonTag.put("name", bluetoothDevice.getName());
                        jsonTag.put("mac_address", bluetoothDevice.getAddress());
                        Plugin.saveSmartTag(getApplicationContext(), bluetoothDevice.getAddress(), jsonTag);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    mDeviceList.remove(bluetoothDevice);
                    Plugin.removeSmartTag(getApplicationContext(), bluetoothDevice.getAddress());
                }
            }
        });
        devicePicker.addView(smartag);
    }

    // Method used to find BLE Devices in old versions of Android
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
                    addToView(bluetoothDevice);
                }
            };

    // Use GATT to connect to selected device from the Radio View
    public void connectToDevice(BluetoothDevice bluetoothDevice) {
        mGatt = bluetoothDevice.connectGatt(this, false, gattCallback);
        scanLeDevice(false);
        Toast.makeText(getApplicationContext(), "Connected to" + bluetoothDevice.getName(), Toast.LENGTH_SHORT)
                .show();
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }
        }

        /** Crux of the App. Before we can start receiving data from the sensor, we must send NOTIFY
         * requests to each of the sensors we are interested in. This will ensure we receive data
         * continuously. Post this request, we have to actually ENABLE the sensors to get them
         * started on transmitting the values. The catch however lies in the fact that Android
         * silently ignores subsequent GATT requests if the previous one doesn't complete. To avoid
         * that and to get all sensors working, we put the thread to sleep for a short time period
         * which allows the previously issued request to complete.
         * */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            final List<BluetoothGattService> mServiceList = mGatt.getServices();

            Thread worker = new Thread(new Runnable() {
                @Override
                public void run() {

                    for (BluetoothGattService service : mServiceList) {

                        String serviceUUID = service.getUuid().toString();

                        // Check if the retrieved service is the move service
                        if (serviceUUID.compareTo(SensorTagGatt.UUID_MOV_SERV.toString()) == 0) {
                            enableNotifications(service, SensorTagGatt.UUID_MOV_DATA);
                            safeSleep();

                            enableMotionService(service, SensorTagGatt.UUID_MOV_CONF, true);
                            safeSleep();
                        } else if (serviceUUID.compareTo(SensorTagGatt.UUID_HUM_SERV.toString()) == 0) {
                            enableNotifications(service, SensorTagGatt.UUID_HUM_DATA);
                            safeSleep();

                            //changePeriod(service, SensorTagGatt.UUID_HUM_PERI, sensorPeriod);
                            enableService(service, SensorTagGatt.UUID_HUM_CONF);
                            safeSleep();

                        } else if (serviceUUID.compareTo(SensorTagGatt.UUID_IRT_SERV.toString()) == 0) {
                            enableNotifications(service, SensorTagGatt.UUID_IRT_DATA);
                            safeSleep();

                            enableService(service, SensorTagGatt.UUID_IRT_CONF);
                            safeSleep();

                        } else if (serviceUUID.compareTo(SensorTagGatt.UUID_OPT_SERV.toString()) == 0) {
                            enableNotifications(service, SensorTagGatt.UUID_OPT_DATA);
                            safeSleep();

                            enableService(service, SensorTagGatt.UUID_OPT_CONF);
                            safeSleep();

                        } else if (serviceUUID.compareTo(SensorTagGatt.UUID_BAR_SERV.toString()) == 0) {
                            enableNotifications(service, SensorTagGatt.UUID_BAR_DATA);
                            safeSleep();

                            enableService(service, SensorTagGatt.UUID_BAR_CONF);
                            safeSleep();
                        }
                    }
                }
            });
            worker.start();
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i("Characteristic written:", characteristic.getUuid().toString());
        }

        /** We receive each of the sensor values here as a byte stream. These must be converted into
         * human readable values. */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            convertData(characteristic, gatt);
        }
    };

    // Convert characteristic data into byte stream and then insert into database for further operations
    private void convertData(BluetoothGattCharacteristic characteristic, BluetoothGatt gatt) {
        String characteristicUUID = characteristic.getUuid().toString();
        byte[] value = characteristic.getValue();
        double lastRead = System.currentTimeMillis();
        if (characteristicUUID.compareTo(SensorTagGatt.UUID_MOV_DATA.toString()) == 0) {

            data = SensorConversion.MOVEMENT_ACC.convert(value);
            try {
                JSONObject json = new JSONObject();
                json.put("sensor_mac", gatt.getDevice().getAddress());
                json.put("accel_x", data.getX());
                json.put("accel_y", data.getY());
                json.put("accel_z", data.getZ());
                json.put("accel_hz", 10);
                json.put("accel_magnitude", data.getCombined());
                json.put("accel_unit", "G");

                Intent record = new Intent(getApplicationContext(), Plugin.class);
                record.setAction(Plugin.ACTION_RECORD_SENSORTAG);
                record.putExtra("sensor", "accelerometer");
                record.putExtra("data", json.toString());
                startService(record);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            data = SensorConversion.MOVEMENT_GYRO.convert(value);
            try {
                JSONObject json = new JSONObject();
                json.put("sensor_mac", gatt.getDevice().getAddress());
                json.put("gyro_x", data.getX());
                json.put("gyro_y", data.getY());
                json.put("gyro_z", data.getZ());
                json.put("gyro_hz", 10);
                json.put("gyro_unit", "degrees");

                Intent record = new Intent(getApplicationContext(), Plugin.class);
                record.setAction(Plugin.ACTION_RECORD_SENSORTAG);
                record.putExtra("sensor", "gyroscope");
                record.putExtra("data", json.toString());
                startService(record);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            data = SensorConversion.MOVEMENT_MAG.convert(value);
            try {
                JSONObject json = new JSONObject();
                json.put("sensor_mac", gatt.getDevice().getAddress());
                json.put("magneto_x", data.getX());
                json.put("magneto_y", data.getY());
                json.put("magneto_z", data.getZ());
                json.put("magneto_hz", 10);
                json.put("magneto_unit", "uT");

                Intent record = new Intent(getApplicationContext(), Plugin.class);
                record.setAction(Plugin.ACTION_RECORD_SENSORTAG);
                record.putExtra("sensor", "magnetometer");
                record.putExtra("data", json.toString());
                startService(record);

            } catch (JSONException e) {
                e.printStackTrace();
            }

        } else if (characteristicUUID.compareTo(SensorTagGatt.UUID_HUM_DATA.toString()) == 0) {
            data = SensorConversion.HUMIDITY2.convert(value);
            try {
                JSONObject json = new JSONObject();
                json.put("sensor_mac", gatt.getDevice().getAddress());
                json.put("humidity", data.getX());
                json.put("humidity_unit", "rg");

                Intent record = new Intent(getApplicationContext(), Plugin.class);
                record.setAction(Plugin.ACTION_RECORD_SENSORTAG);
                record.putExtra("sensor", "humidity");
                record.putExtra("data", json.toString());
                startService(record);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (characteristicUUID.compareTo(SensorTagGatt.UUID_IRT_DATA.toString()) == 0) {

            data = SensorConversion.IR_TEMPERATURE.convert(value);
            try {
                JSONObject json = new JSONObject();
                json.put("sensor_mac", gatt.getDevice().getAddress());
                json.put("ambient_temperature", data.getX());
                json.put("ambient_unit", "celsius");

                Intent record = new Intent(getApplicationContext(), Plugin.class);
                record.setAction(Plugin.ACTION_RECORD_SENSORTAG);
                record.putExtra("sensor", "ambient_temperature");
                record.putExtra("data", json.toString());
                startService(record);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                JSONObject json = new JSONObject();
                json.put("sensor_mac", gatt.getDevice().getAddress());
                json.put("target_temperature", data.getZ());
                json.put("target_unit", "celsius");

                Intent record = new Intent(getApplicationContext(), Plugin.class);
                record.setAction(Plugin.ACTION_RECORD_SENSORTAG);
                record.putExtra("sensor", "target_temperature");
                record.putExtra("data", json.toString());
                startService(record);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (characteristicUUID.compareTo(SensorTagGatt.UUID_OPT_DATA.toString()) == 0) {
            data = SensorConversion.LUXOMETER.convert(value);
            try {
                JSONObject json = new JSONObject();
                json.put("sensor_mac", gatt.getDevice().getAddress());
                json.put("light", data.getX());
                json.put("light_unit", "lux");

                Intent record = new Intent(getApplicationContext(), Plugin.class);
                record.setAction(Plugin.ACTION_RECORD_SENSORTAG);
                record.putExtra("sensor", "light");
                record.putExtra("data", json.toString());
                startService(record);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (characteristicUUID.compareTo(SensorTagGatt.UUID_BAR_DATA.toString()) == 0) {
            data = SensorConversion.BAROMETER.convert(value);
            try {
                JSONObject json = new JSONObject();
                json.put("sensor_mac", gatt.getDevice().getAddress());
                json.put("barometer", data.getX()/100);
                json.put("barometer_unit", "mBar");

                Intent record = new Intent(getApplicationContext(), Plugin.class);
                record.setAction(Plugin.ACTION_RECORD_SENSORTAG);
                record.putExtra("sensor", "barometer");
                record.putExtra("data", json.toString());
                startService(record);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Motion (Accelerometer/Gyro/Mag) service has a different structure to it and requires a
     * separate function to enable
     */
    private void enableMotionService(BluetoothGattService service, UUID uuidMovConf, boolean bool) {
        byte b[] = new byte[]{0x7F, 0x00};
        // 0x7F (hexadecimal) = 127 (decimal)
        // 127 = 2^0 + 2^1 ... 2^6
        // Enables all bits from 0-6

        if (bool) {
            b[0] = (byte) 0xFF;  // Enables bit 7
        }

        // Get Configuration Characteristic
        BluetoothGattCharacteristic config = service.getCharacteristic(uuidMovConf);
        config.setValue(b);
        mGatt.writeCharacteristic(config);
    }

    /* Issue notify requests */
    private void enableNotifications(BluetoothGattService service, UUID uuidData) {
        BluetoothGattCharacteristic dataCharacteristic = service.getCharacteristic(uuidData);
        mGatt.setCharacteristicNotification(dataCharacteristic, true);

        BluetoothGattDescriptor config = dataCharacteristic.getDescriptor(SensorTagGatt.UUID_NOTIFICATIONS);
        config.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mGatt.writeDescriptor(config);
    }

    /* Put thread to sleep */
    private void safeSleep() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /* Enable service request */
    private void enableService(BluetoothGattService service, UUID uuidConf) {
        BluetoothGattCharacteristic config = service.getCharacteristic(uuidConf);
        config.setValue(new byte[]{1});
        mGatt.writeCharacteristic(config);

    }

    /*private void changePeriod(BluetoothGattService service, UUID periodUUID, byte p) {
        BluetoothGattCharacteristic periodCharacteristic = service.getCharacteristic(periodUUID);

        byte[] val = new byte[1];
        val[0] = p;

        periodCharacteristic.setValue(p);
        mGatt.writeCharacteristic(periodCharacteristic);
    }*/
}
