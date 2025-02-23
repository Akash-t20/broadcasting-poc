package com.nativelocalstorage;
import static androidx.core.app.ActivityCompat.*;

import static com.facebook.common.util.Hex.hexStringToByteArray;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.PeriodicAdvertisingParameters;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import java.util.UUID;

public class NativeLocalStorageModule extends ReactContextBaseJavaModule {


    private BluetoothLeAdvertiser advertiser;
    private AdvertisingSet currentAdvertisingSet;//.....
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
        try {
            Log.d(TAG2, "Starting broadcasting");
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter(); //BluetoothAdapter is your primary way to interact with Bluetooth functionality on Android, if the device doesn't have Bluetooth  it will  return null.

            if (adapter == null) {
                promise.reject("BLUETOOTH_UNAVAILABLE", "Bluetooth adapter not available");
                return;
            }

            // checking condition that not all Bluetooth-enabled devices support BLE advertising.
            // Some devices might only support classic Bluetooth or BLE scanning but not advertising.

            advertiser = adapter.getBluetoothLeAdvertiser();
            if (advertiser == null) {
                promise.reject("ADVERTISING_UNSUPPORTED", "Bluetooth LE advertising not supported");
                return;
            }

            //The 2M PHY is a feature that allows for higher data transfer speeds (2 Mbps) compared to the original 1M PHY (1 Mbps)
            //introduced in Bluetooth 5-> large amount data, && for for periodic advertising and other advanced advertising features.
            if (!adapter.isLeExtendedAdvertisingSupported()) {
                promise.reject("EXTENDED_ADVERTISING_UNSUPPORTED", "Extended Advertising not supported!");
                return;
            }


            int maxDataLength = 0;
            maxDataLength = adapter.getLeMaximumAdvertisingDataLength();// This length includes all data in the advertisement, such as flags, manufacturer data, service UUIDs, service data,
            Log.i(TAG, "Max advertising data length: " + maxDataLength);

            AdvertisingSetParameters parameters = getAdvertisingSetParameters();

            // Manufacturer Specific Data
            int manufacturerId = 0x5450; //change id as wanted
            String hexData = "A39501FFA5AF00000101"; // change data required ;
            byte[] manufacturerData = hexStringToByteArray(hexData);  // change data required ;

            AdvertiseData data = new AdvertiseData.Builder()
                    //.addServiceData(new ParcelUuid(myServiceUUID), "SWITCH-ON".getBytes())
                    .setIncludeDeviceName(true) // identify your device in scans.
                    .addManufacturerData(manufacturerId, manufacturerData)
                    .build();
            
              advertisingCallback = new AdvertisingSetCallback() {
                @Override
                public void onAdvertisingSetStarted(AdvertisingSet advertisingSet, int txPower, int status) {
                    Log.i(TAG2, "Advertising started: txPower=" + txPower + ", status=" + status);
                    Log.i(TAG2, "Protco Broadcasting Started: txPower=" + data.toString());

                    if (status == AdvertisingSetCallback.ADVERTISE_SUCCESS) {
                        currentAdvertisingSet = advertisingSet;
                        Log.i(TAG2, "Manufacture Data passed succesfuuly");
                        promise.resolve(null);


                    } else {
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
            advertiser.startAdvertisingSet(parameters, data, null, null, null, advertisingCallback);
            Log.e(TAG2, "Method called -> startAdvertisingSet ");

        } catch (Exception e) {
            promise.reject("ERROR", "Error starting advertising: " + e.getMessage());
            Log.e(TAG, "Error in startAdvertising", e);
        }
    }

    private static AdvertisingSetParameters getAdvertisingSetParameters() {
        AdvertisingSetParameters parameters = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            parameters = new AdvertisingSetParameters.Builder()
                    .setLegacyMode(false)
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
            advertiser.stopAdvertisingSet(advertisingCallback);
            currentAdvertisingSet = null;
            Log.i(TAG, "Advertising stopped successfully");
        } else {
            Log.e(TAG, "Cannot stop advertising, advertiser or callback is null");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @ReactMethod
    public void updateAdvertisingData(boolean turnOn, Promise promise) {
        if (currentAdvertisingSet == null) {
            Log.e(TAG2, "Error: No active advertising set to update.");
            promise.reject("NO_ADVERTISING", "No active advertising set to update.");
            return;
        }

        try {
            //  (ON or OFF) toggling
            String hexData = turnOn ? "A39501FFA5AF00000101" : "A39501FF6E7800000102";
            byte[] manufacturerData = hexStringToByteArray(hexData);

            AdvertiseData newData = new AdvertiseData.Builder()
                    .setIncludeDeviceName(true)
                    .addManufacturerData(0x5450, manufacturerData)
                    .build();

            //  Advertise Permission
            if (ActivityCompat.checkSelfPermission(this.getReactApplicationContext(), Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG2, "Bluetooth Advertise Permission Not Granted");
                promise.reject("PERMISSION_DENIED", "BLUETOOTH_ADVERTISE permission not granted.");
                return;
            }


            currentAdvertisingSet.setAdvertisingData(newData);
            Log.i(TAG2, "Successfully updated Advertising Data to " + (turnOn ? "ON" : "OFF"));

            promise.resolve(true);
        } catch (IllegalStateException e) {
            Log.e(TAG2, "Advertising data update failed: " + e.getMessage());
            promise.reject("UPDATE_FAILED", "IllegalStateException: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG2, "Failed to update advertising data: " + e.getMessage());
            promise.reject("UPDATE_FAILED", "Error updating advertising data: " + e.getMessage());
        }
    }
}


