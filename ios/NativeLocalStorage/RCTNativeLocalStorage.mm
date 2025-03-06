#import "RCTNativeLocalStorage.h"
#import <React/RCTLog.h>
#import <CoreBluetooth/CoreBluetooth.h>

@interface RCTNativeLocalStorage() <CBPeripheralManagerDelegate>
@property (nonatomic, strong) CBPeripheralManager *peripheralManager;
@property (nonatomic, assign) BOOL isAdvertising;
@property (nonatomic, assign) BOOL isAdvertisingDataOn;
@property (nonatomic, strong) CBMutableCharacteristic *readCharacteristic;
@property (nonatomic, strong) CBMutableCharacteristic *writeCharacteristic;
@property (nonatomic, strong) CBMutableService *customService;
@end

@implementation RCTNativeLocalStorage

RCT_EXPORT_MODULE(NativeLocalStorage)

- (instancetype)init {
    if (self = [super init]) {
        _peripheralManager = [[CBPeripheralManager alloc] initWithDelegate:self queue:nil];
        _isAdvertising = NO;
        _isAdvertisingDataOn = YES;
    }
    return self;
}

// ✅ Check if Bluetooth is enabled
RCT_EXPORT_METHOD(isBluetoothEnabled:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject) {
    BOOL bluetoothEnabled = (_peripheralManager.state == CBManagerStatePoweredOn);
    resolve(@(bluetoothEnabled));
}

// ✅ Start BLE Advertising (Ensures state is properly updated)
RCT_EXPORT_METHOD(startAdvertising:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject) {
    if (_peripheralManager.state != CBManagerStatePoweredOn) {
        reject(@"BLUETOOTH_DISABLED", @"Bluetooth is not enabled", nil);
        return;
    }
    
    if (_isAdvertising) {
        RCTLogInfo(@"⚠️ Already advertising, ignoring request.");
        resolve(@(YES));
        return;
    }

    [self setupBLEServiceAndCharacteristics]; // Setup services
}

// ✅ Stop BLE Advertising (Ensures state is properly updated)
RCT_EXPORT_METHOD(stopAdvertising:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject) {
    if (!_isAdvertising) {
        RCTLogInfo(@"⚠️ No active advertising to stop.");
        resolve(@(YES));
        return;
    }

    [_peripheralManager stopAdvertising];
    [_peripheralManager removeAllServices];  // 🔥 Stops all services & characteristics
    _isAdvertising = NO;

    RCTLogInfo(@"🛑 Stopped Advertising and Removed Services");
    resolve(@(YES));
}

// ✅ Update BLE Advertising Data (ON/OFF toggle)
RCT_EXPORT_METHOD(updateAdvertisingData:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject) {
    if (!_isAdvertising) {
        reject(@"NOT_ADVERTISING", @"Advertising must be started first", nil);
        return;
    }

    _isAdvertisingDataOn = !_isAdvertisingDataOn;
    NSString *newHexData = _isAdvertisingDataOn ? @"B264EE063CB9" : @"A39501FF6E72";

    // Convert hex string to NSData
    NSMutableData *manufacturerData = [NSMutableData new];
    uint16_t companyID = CFSwapInt16HostToLittle(0x6624); // Manufacturer ID
    [manufacturerData appendBytes:&companyID length:sizeof(companyID)];

    for (NSInteger i = 0; i < newHexData.length; i += 2) {
        NSString *hexByte = [newHexData substringWithRange:NSMakeRange(i, 2)];
        unsigned int byteValue;
        [[NSScanner scannerWithString:hexByte] scanHexInt:&byteValue];
        uint8_t byte = (uint8_t)byteValue;
        [manufacturerData appendBytes:&byte length:1];
    }

    // Update advertising data without stopping
    NSDictionary *advertisementData = @{
        CBAdvertisementDataManufacturerDataKey: manufacturerData,
        CBAdvertisementDataLocalNameKey: @"BLEPeripheral"
    };

    [_peripheralManager stopAdvertising];

    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 1.0 * NSEC_PER_SEC), dispatch_get_main_queue(), ^{
        [_peripheralManager startAdvertising:advertisementData];
        RCTLogInfo(@"🔄 Updated Advertising Data: %@", newHexData);
        resolve(@(YES));
    });
}

// ✅ Bluetooth State Handling
- (void)peripheralManagerDidUpdateState:(CBPeripheralManager *)peripheral {
    if (peripheral.state == CBManagerStatePoweredOn) {
        RCTLogInfo(@"✅ Bluetooth is ON - Ready to advertise");
    } else {
        RCTLogInfo(@"❌ Bluetooth is OFF or Not Ready");
    }
}

// ✅ Set Up BLE Service & Characteristics
- (void)setupBLEServiceAndCharacteristics {
    CBUUID *serviceUUID = [CBUUID UUIDWithString:@"12345678-1234-1234-1234-1234567890AB"];//UUID is how other devices will recognize your service.
    CBUUID *readCharUUID = [CBUUID UUIDWithString:@"12345678-1234-1234-1234-1234567890AC"];//"read" characteristic. This characteristic will allow other devices to read data from your device.
    CBUUID *writeCharUUID = [CBUUID UUIDWithString:@"12345678-1234-1234-1234-1234567890AD"];//write" characteristic. This characteristic will allow other devices to send data to your device.

    // ✅ Read Characteristic
    _readCharacteristic = [[CBMutableCharacteristic alloc] initWithType:readCharUUID
                                                             properties:CBCharacteristicPropertyRead | CBCharacteristicPropertyNotify
                                                                  value:nil
                                                            permissions:CBAttributePermissionsReadable];

    // ✅ Write Characteristic
    _writeCharacteristic = [[CBMutableCharacteristic alloc] initWithType:writeCharUUID
                                                              properties:CBCharacteristicPropertyWrite
                                                                   value:nil
                                                             permissions:CBAttributePermissionsWriteable];

    // ✅ Service Creation
    _customService = [[CBMutableService alloc] initWithType:serviceUUID primary:YES];
    _customService.characteristics = @[_writeCharacteristic,_readCharacteristic];

    // ✅ Add Service to Peripheral Manager
    [_peripheralManager addService:_customService];

    // ✅ Start Advertising
    [self startBLEAdvertising];
}

- (void)startBLEAdvertising {
    uint16_t companyID = CFSwapInt16HostToLittle(0x6624);
    NSData *manufacturerSpecificData = [self dataFromHexString:@"B264EE063CB9"];

    NSMutableData *manufacturerData = [NSMutableData dataWithBytes:&companyID length:sizeof(companyID)];
    [manufacturerData appendData:manufacturerSpecificData];

    // Log the manufacturer data
    NSLog(@"Manufacturer Data: %@", manufacturerData);

    NSDictionary *advertisementData = @{
        CBAdvertisementDataLocalNameKey: @"BLEPeripheral",
        CBAdvertisementDataServiceUUIDsKey: @[_customService.UUID],
        CBAdvertisementDataManufacturerDataKey: manufacturerData
    };

    [_peripheralManager startAdvertising:advertisementData];
    _isAdvertising = YES;

    NSLog(@"🚀 Started Advertising with JBL Manufacturer Data");
}
// Helper function to convert hex string to NSData
- (NSData *)dataFromHexString:(NSString *)hexString {
    NSMutableData *data = [NSMutableData new];
    unsigned char whole_byte;
    char byte_chars[3] = {'\0', '\0', '\0'};
    int i = 0;
    int length = (int)hexString.length / 2;
    for (i = 0; i < length; i++) {
        byte_chars[0] = [hexString characterAtIndex:i * 2];
        byte_chars[1] = [hexString characterAtIndex:i * 2 + 1];
        whole_byte = strtol(byte_chars, NULL, 16);
        [data appendBytes:&whole_byte length:1];
    }
    return data;
}
// ✅ TurboModule Support
- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:(const facebook::react::ObjCTurboModule::InitParams &)params {
    return std::make_shared<facebook::react::NativeLocalStorageSpecJSI>(params);
}

@end
