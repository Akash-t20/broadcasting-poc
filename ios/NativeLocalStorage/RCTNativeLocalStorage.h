//  RCTNativeLocalStorage.h
#import <Foundation/Foundation.h>
#import <React/RCTBridgeModule.h>
#import <ReactCommon/RCTTurboModule.h>
#import <NativeLocalStorageSpec/NativeLocalStorageSpec.h>

NS_ASSUME_NONNULL_BEGIN

@interface RCTNativeLocalStorage : NSObject <NativeLocalStorageSpec, RCTBridgeModule, RCTTurboModule>

@end

NS_ASSUME_NONNULL_END
