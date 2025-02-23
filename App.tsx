import React, { useState, useEffect } from 'react';
import {
  PermissionsAndroid,
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
  const [isAdvertisingDataOn, setIsAdvertisingDataOn] = useState(true); // NEW STATE

  useEffect(() => {
    requestBluetoothPermissions();
    if (Platform.OS === 'android') {
      try {
        NativeLocalStorage?.isBluetoothEnabled().then(setBluetoothEnabled);
      } catch (error) {
        console.log("UseEffect:", error);
      }
    }
  }, []);

  const requestBluetoothPermissions = async () => {
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
        console.log("Granted:", granted);
        console.log("allGranted", allGranted);
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
    if (bluetoothEnabled && advertising) return;
    try {
      await NativeLocalStorage.startAdvertising();
      setAdvertising(true);
      setIsAdvertisingDataOn(true); 
      console.log("Starting Advertising");
    } catch (error) {
      console.error("Error starting advertising:", error);
      Alert.alert("Error", "Error starting advertising. Please try again.");
    }
  };

  const stopAdvertising = async () => {
    if (!bluetoothEnabled || !advertising) return;
    try {
      await NativeLocalStorage.stopAdvertising();
      setAdvertising(false);
      console.log("Stopping Advertising");
    } catch (error) {
      console.error("Error stopping advertising:", error);
      Alert.alert("Error", "Error stopping advertising. Please try again.");
    }
  };

  const toggleAdvertisingData = async () => {
    if (!advertising) {
      Alert.alert("Error", "You must start advertising first!");
      return;
    }
    
    try {
      await NativeLocalStorage.updateAdvertisingData(!isAdvertisingDataOn);
      setIsAdvertisingDataOn(!isAdvertisingDataOn);
      console.log(`Advertising data updated to ${!isAdvertisingDataOn ? "OFF" : "ON"}`);
    } catch (error) {
      console.error("Error updating advertising data:", error);
      Alert.alert("Error", "Failed to update advertising data.");
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
        onPress={() => console.log("Scanning not implemented")}
        disabled={scanning || !bluetoothEnabled}
      />
      <Button
        title={isAdvertisingDataOn ? "Set Advertising Data to OFF" : "Set Advertising Data to ON"}
        onPress={toggleAdvertisingData}
        disabled={!advertising}
      />
      
      <Text>Scanned Devices:</Text>
      {scannedDevices.map((device, index) => (
        <Text key={index}>{device}</Text>
      ))}
    </View>
  );
};

export default App;
