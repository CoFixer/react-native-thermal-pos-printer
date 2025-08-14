#import <React/RCTBridgeModule.h>

@interface PosPrinter : NSObject <RCTBridgeModule>

@end
#ifdef RCT_NEW_ARCH_ENABLED
#import "RNPosPrinterSpec.h"
@interface PosPrinter : NSObject <NativePosPrinterSpec>
#else
#import <React/RCTBridgeModule.h>
#import <CoreBluetooth/CoreBluetooth.h>

@interface PosPrinter : NSObject <RCTBridgeModule, CBCentralManagerDelegate, CBPeripheralDelegate>

@end
#endif