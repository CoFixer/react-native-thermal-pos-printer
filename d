#import <React/RCTBridgeModule.h>
#import <CoreBluetooth/CoreBluetooth.h>

@interface PosPrinter : NSObject <RCTBridgeModule, CBCentralManagerDelegate, CBPeripheralDelegate>

@end