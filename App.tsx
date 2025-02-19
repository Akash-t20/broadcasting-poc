/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 */
import React, { useState, useEffect } from 'react';
import {
  PermissionsAndroid, // Import PermissionsAndroid
  Platform,
  Text,
  Button,
  View,
  Alert, 
  NativeModules 
} from 'react-native';

import NativeLocalStorage from './specs/NativeLocalStorage';

const App: React.FC = () => {
  const [bluetoothEnabled, setBluetoothEnabled] = useState(false);
  const [advertising, setAdvertising] = useState(false);
  const [scanning, setScanning] = useState(false);
  const [scannedDevices, setScannedDevices] = useState<string[]>([]);

  useEffect(() => {
    requestBluetoothPermissions();
    if (Platform.OS === 'android') {
      try {
        NativeLocalStorage?.isBluetoothEnabled().then(setBluetoothEnabled)
      } catch (error) {
        console.log("UseEffect:",error)
      }
      
    }
  }, []);

  const requestBluetoothPermissions = async () => { // async operation Many operations in programming take time to complete.  Think of fetching data from a server, reading a file, or waiting for user input.  These operations are asynchronous – they don't block the rest of your code from running while they're in progress.
    if (Platform.OS === 'android') {
      try {
        const granted = await PermissionsAndroid.requestMultiple([
          PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
          PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
          PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
          PermissionsAndroid.PERMISSIONS.BLUETOOTH_ADVERTISE,
          PermissionsAndroid.PERMISSIONS.ACCESS_COARSE_LOCATION,
        ]);

        

        const allGranted = Object.values(granted).every(result => result === PermissionsAndroid.RESULTS.GRANTED);
        console.log("Granted:",granted)
        console.log("allGranted",allGranted)
        if (allGranted) {
          setBluetoothEnabled(true);
        } else {
          console.log("Bluetooth permissions denied");
          Alert.alert("Permissions Denied", "Bluetooth permissions are required for this app.");
        }
      } catch (err) {
        console.error("Error requesting permissions:", err);
        Alert.alert("Error", "Error requesting permissions. Please try again.");
      }
    } else {
      setBluetoothEnabled(true);
    }
  };

  const startAdvertising = async () => {
    if (bluetoothEnabled && advertising) return;  //( !bluetoothEnabled || advertising)
    try {
      const deviceName = " Akash";
      await NativeLocalStorage.startAdvertising();
      setAdvertising(true);
      console.log("Starting Advertising with device name:", deviceName);
    } catch (error) {
      console.error("Error starting advertising:", error);
      Alert.alert("Error", "Error starting advertising. Please try again.");
    }
  };

  const stopAdvertising = async () => {
    if (!bluetoothEnabled || !advertising) return; // (!advertising)
    try {
      await NativeLocalStorage.stopAdvertising();
      setAdvertising(false);
      console.log("Stopping Advertising");
    } catch (error) {
      console.error("Error stopping advertising:", error);
      Alert.alert("Error", "Error stopping advertising. Please try again.");
    }
  };

  const startScanning = async () => {
    if (bluetoothEnabled && scanning) return;
    try {
      setScanning(true);
      setScannedDevices([]);
      const devices = await NativeLocalStorage.startScanning();
      console.log("Starting Scanning");
    } catch (error) {
      console.error("Error starting scanning:", error);
      Alert.alert("Error", "Error starting scanning. Please try again.");
    }
  };
return (
  <View>
    <Button
      title="Start Advertising"
      onPress={startAdvertising}
      disabled={advertising || !bluetoothEnabled}
    />
    <Button
      title="Stop Advertising"
      onPress={stopAdvertising}
      disabled={!advertising}
    />
    <Button
      title="Start Scanning"
      onPress={startScanning}
      disabled={scanning || !bluetoothEnabled}
    />
    
    <Text>Scanned Devices:</Text>
    {scannedDevices.map((device, index) => (
      <Text key={index}>{device}</Text>
    ))}
  </View>
);
};

export default App;