#if __has_include(<React/RCTBridgeModule.h>)
#import <React/RCTBridgeModule.h>
#elif __has_include("RCTBridgeModule.h")
#import "RCTBridgeModule.h"
#elif __has_include("React/RCTBridgeModule.h")
#import "React/RCTBridgeModule.h"
#else
#import <Foundation/Foundation.h>
@protocol RCTBridgeModule <NSObject>
@end
#endif

#import <CoreBluetooth/CoreBluetooth.h>

@interface PosPrinter : NSObject <RCTBridgeModule, CBCentralManagerDelegate, CBPeripheralDelegate>

@end