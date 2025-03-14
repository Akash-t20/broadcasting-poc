import type {TurboModule} from 'react-native';
import {TurboModuleRegistry} from 'react-native';

export interface Spec extends TurboModule {

    isBluetoothEnabled(): Promise<boolean>;
    requestBluetoothEnable(): Promise<boolean>;
    startAdvertising(): Promise<boolean>;
    stopAdvertising(): Promise<void>;
    startScanning(): Promise<boolean>;
    getScannedDevices(): Promise<Array<{ name: string; address: string }>>;
    updateAdvertisingData(isDataOn: boolean): Promise<void>;  // Accepts a boolean to switch data on or off.
}

export default TurboModuleRegistry.getEnforcing<Spec>(
  'NativeLocalStorage',
);
