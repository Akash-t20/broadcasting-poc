package com.nativelocalstorage;

import static com.facebook.common.util.Hex.hexStringToByteArray;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
public class NativeLocalStorageModule extends ReactContextBaseJavaModule {


    private BluetoothLeAdvertiser advertiser;
    private AdvertisingSet currentAdvertisingSet;
    private AdvertisingSetCallback advertisingCallback;

    private static final String TAG = "NativeLocalStorage";
    private static final String TAG2 = "Protco Broadcaster";



    public NativeLocalStorageModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @NonNull
    @Override
    public String getName() {
        return "NativeLocalStorage";
    }



    @RequiresApi(api = Build.VERSION_CODES.S)
    @ReactMethod
    public void startAdvertising(Promise promise) {
        promise.resolve(true); // asynchronous operations-> promise.resolve(), the Promise's state changes from "pending" to "fulfilled"
        try {
            Log.d(TAG2,"Starting broadcasting");
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            //BluetoothAdapter is your primary way to interact with Bluetooth functionality on Android, if the device doesn't have Bluetooth  it will  return null.

            if (adapter == null) {
                promise.reject("BLUETOOTH_UNAVAILABLE", "Bluetooth adapter not available");
                return;
            }

            // checking condition that not all Bluetooth-enabled devices support BLE advertising.
            // Some devices might only support classic Bluetooth or BLE scanning but not advertising.

            if (advertiser == null) {
                advertiser = adapter.getBluetoothLeAdvertiser();
            }

            if (advertiser == null) {
                 promise.reject("ADVERTISING_UNSUPPORTED", "Bluetooth LE advertising not supported");
                return;
            }


            int maxDataLength = 0;
            maxDataLength = adapter.getLeMaximumAdvertisingDataLength(); // This length includes all data in the advertisement, such as flags, manufacturer data, service UUIDs, service data,
            Log.i(TAG, "Max advertising data length: " + maxDataLength);

            AdvertisingSetParameters parameters = getAdvertisingSetParameters();

            // Manufacturer Specific Data
            int manufacturerId = 0x5450; //change ID
            String hexData = "A39501FFA5AF00000101"; // change Data
            byte[] manufacturerData = hexStringToByteArray(hexData);

            //For service Data
            // UUID myServiceUUID = UUID.fromString("cb7d58a6-854e-4ffa-872e-251a1b02e164"); // uuid changes

            AdvertiseData data = new AdvertiseData.Builder()

                    //.addServiceData(new ParcelUuid(myServiceUUID), "SWITCH-ON".getBytes())  // for service Data

                    .setIncludeDeviceName(true)
                    .addManufacturerData(manufacturerId, manufacturerData)
                    .build();


            advertisingCallback = new AdvertisingSetCallback() {
                @Override
                public void onAdvertisingSetStarted(AdvertisingSet advertisingSet, int txPower, int status) {
                    Log.i(TAG2, "Advertising started: txPower=" + txPower + ", status=" + status);
                    Log.i(TAG2, "Protco Broadcasting Started: txPower=" );
                    if (status == AdvertisingSetCallback.ADVERTISE_SUCCESS) {
                        currentAdvertisingSet = advertisingSet;

//                        // Adding handler for to stop broadcasting in 3 sec (change time as u want)
//                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
//                            stopAdvertising(promise);
//                        }, 10000);
                     promise.resolve(null);

                    }
                    else {
                        promise.reject("ADVERTISING_START_FAILED", "Advertising start failed: " + status);
                        Log.e(TAG2, "Advertising start failed: " + status);
                    }
                }

                @Override
                public void onAdvertisingSetStopped(AdvertisingSet advertisingSet) {
                    Log.i(TAG2, "Advertising stopped");
                    currentAdvertisingSet = null;
                }
            };

            if (ActivityCompat.checkSelfPermission(this.getReactApplicationContext(), Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            advertiser.startAdvertisingSet(parameters,data,null,null,null, advertisingCallback);
            Log.e(TAG2, "Method called -> startAdvertisingSet ");
            return;

        } catch (Exception e) {
            promise.reject("ERROR", "Error starting advertising: " + e.getMessage());
            Log.e(TAG, "Error in startAdvertising", e);
        }
//        promise.resolve(true);
    }
    private static AdvertisingSetParameters getAdvertisingSetParameters() {
        AdvertisingSetParameters parameters = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            parameters = new AdvertisingSetParameters.Builder()
                    .setLegacyMode(false) // for legacy mode
                    .setConnectable(false) // You only need to broadcast data && You don't need two-way communication.
                    .setInterval(AdvertisingSetParameters.INTERVAL_LOW)//  your device will advertise very frequently && making it easier and faster for other devices to discover it.
                    .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
                    .build();
        }
        return parameters;
    }

    @ReactMethod
    public void stopAdvertising(Promise promise) {
        promise.resolve(true);

        if (advertiser != null && advertisingCallback != null
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            if (ActivityCompat.checkSelfPermission(this.getReactApplicationContext(), Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
           // OFF data here passed
            if (currentAdvertisingSet!=null){
                int manufacturerId = 0x5450; // Same manufacturer ID
                String offHexData = "A39501FF6E7800000102"; // OFF data
                byte[] offData = hexStringToByteArray(offHexData);

                AdvertiseData offAdvertiseData = new AdvertiseData.Builder()
                        .addManufacturerData(manufacturerId, offData)
                        .build();

                currentAdvertisingSet.setAdvertisingData(offAdvertiseData);
                Log.i(TAG, "Passing  OFF data before stopping advertising");


            } else {
                advertiser.stopAdvertisingSet(advertisingCallback);
                currentAdvertisingSet = null;
                Log.i(TAG, "Advertising stopped successfully without OFF data");
            }
        }
    }

}

