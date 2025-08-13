#import "PosPrinter.h"
#import <CoreBluetooth/CoreBluetooth.h>

#ifdef RCT_NEW_ARCH_ENABLED
#import "RNPosPrinterSpec.h"
#else
#import <React/RCTBridgeModule.h>
#import <React/RCTLog.h>
#endif

@implementation PosPrinter {
    CBCentralManager *centralManager;
    CBPeripheral *connectedPeripheral;
    CBCharacteristic *writeCharacteristic;
    BOOL isConnected;
    NSMutableArray *discoveredDevices;
    RCTPromiseResolveBlock deviceListResolve;
    RCTPromiseRejectBlock deviceListReject;
}

RCT_EXPORT_MODULE()

- (instancetype)init {
    self = [super init];
    if (self) {
        centralManager = [[CBCentralManager alloc] initWithDelegate:self queue:nil];
        isConnected = NO;
        discoveredDevices = [[NSMutableArray alloc] init];
    }
    return self;
}

RCT_EXPORT_METHOD(init:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    resolve(@YES);
}

RCT_EXPORT_METHOD(getDeviceList:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    NSMutableArray *devices = [[NSMutableArray alloc] init];
    
    if (centralManager.state != CBManagerStatePoweredOn) {
        reject(@"BLUETOOTH_OFF", @"Bluetooth is not powered on", nil);
        return;
    }
    
    // Get previously connected/paired peripherals
    NSArray *knownPeripherals = [centralManager retrieveConnectedPeripheralsWithServices:@[]];
    
    // Also get peripherals with known identifiers (if you have stored them)
    // NSArray *retrievedPeripherals = [centralManager retrievePeripheralsWithIdentifiers:@[/* stored UUIDs */]];
    
    for (CBPeripheral *peripheral in knownPeripherals) {
        NSDictionary *device = @{
            @"name": peripheral.name ?: @"Unknown Device",
            @"address": peripheral.identifier.UUIDString,
            @"type": @"BLUETOOTH",
            @"connected": @(peripheral.state == CBPeripheralStateConnected)
        };
        [devices addObject:device];
    }
    
    resolve(devices);
}
    deviceListResolve = resolve;
    deviceListReject = reject;
    
    if (centralManager.state != CBManagerStatePoweredOn) {
        reject(@"BLUETOOTH_OFF", @"Bluetooth is not powered on", nil);
        return;
    }
    
    [discoveredDevices removeAllObjects];
    
    // Start scanning for peripherals
    [centralManager scanForPeripheralsWithServices:nil options:nil];
    
    // Stop scanning after 10 seconds and return results
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(10.0 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        [self->centralManager stopScan];
        
        NSMutableArray *devices = [[NSMutableArray alloc] init];
        for (CBPeripheral *peripheral in self->discoveredDevices) {
            NSDictionary *device = @{
                @"name": peripheral.name ?: @"Unknown Device",
                @"address": peripheral.identifier.UUIDString,
                @"type": @"BLUETOOTH",
                @"connected": @(peripheral.state == CBPeripheralStateConnected)
            };
            [devices addObject:device];
        }
        
        if (self->deviceListResolve) {
            self->deviceListResolve(devices);
            self->deviceListResolve = nil;
            self->deviceListReject = nil;
        }
    });
}

#pragma mark - CBCentralManagerDelegate

- (void)centralManagerDidUpdateState:(CBCentralManager *)central {
    if (central.state == CBManagerStatePoweredOn) {
        // Bluetooth is ready
    } else {
        if (deviceListReject) {
            deviceListReject(@"BLUETOOTH_ERROR", @"Bluetooth is not available", nil);
            deviceListResolve = nil;
            deviceListReject = nil;
        }
    }
}

- (void)centralManager:(CBCentralManager *)central didDiscoverPeripheral:(CBPeripheral *)peripheral advertisementData:(NSDictionary<NSString *,id> *)advertisementData RSSI:(NSNumber *)RSSI {
    if (![discoveredDevices containsObject:peripheral]) {
        [discoveredDevices addObject:peripheral];
    }
}

RCT_EXPORT_METHOD(connectPrinter:(NSString *)address
                  type:(NSString *)type
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    resolve(@YES);
}

RCT_EXPORT_METHOD(disconnectPrinter:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    isConnected = NO;
    resolve(@YES);
}

RCT_EXPORT_METHOD(isConnected:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    resolve(@(isConnected));
}

RCT_EXPORT_METHOD(printText:(NSString *)text
                  options:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    resolve(@YES);
}

RCT_EXPORT_METHOD(printImage:(NSString *)base64
                  options:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    if (!isConnected || !writeCharacteristic) {
        reject(@"NOT_CONNECTED", @"Printer is not connected", nil);
        return;
    }
    
    NSData *imageData = [[NSData alloc] initWithBase64EncodedString:base64 options:0];
    if (!imageData) {
        reject(@"INVALID_IMAGE", @"Invalid base64 image data", nil);
        return;
    }
    
    UIImage *image = [UIImage imageWithData:imageData];
    if (!image) {
        reject(@"INVALID_IMAGE", @"Cannot create image from data", nil);
        return;
    }
    
    NSData *escPosData = [self convertImageToEscPos:image];
    
    [connectedPeripheral writeValue:escPosData
                  forCharacteristic:writeCharacteristic
                               type:CBCharacteristicWriteWithoutResponse];
    
    resolve(@YES);
}

- (NSData *)convertImageToEscPos:(UIImage *)image {
    CGFloat printerWidth = 384.0;
    CGFloat ratio = printerWidth / image.size.width;
    CGSize newSize = CGSizeMake(printerWidth, image.size.height * ratio);
    
    UIGraphicsBeginImageContext(newSize);
    [image drawInRect:CGRectMake(0, 0, newSize.width, newSize.height)];
    UIImage *resizedImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    return [self convertToRasterFormat:resizedImage];
}

- (NSData *)convertToRasterFormat:(UIImage *)image {
    CGImageRef cgImage = image.CGImage;
    NSUInteger width = CGImageGetWidth(cgImage);
    NSUInteger height = CGImageGetHeight(cgImage);
    
    NSUInteger bytesPerPixel = 4;
    NSUInteger bytesPerRow = bytesPerPixel * width;
    NSUInteger bitsPerComponent = 8;
    
    unsigned char *rawData = (unsigned char*) calloc(height * width * bytesPerPixel, sizeof(unsigned char));
    
    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    CGContextRef context = CGBitmapContextCreate(rawData, width, height,
                                                bitsPerComponent, bytesPerRow, colorSpace,
                                                kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big);
    CGColorSpaceRelease(colorSpace);
    
    CGContextDrawImage(context, CGRectMake(0, 0, width, height), cgImage);
    CGContextRelease(context);
    
    NSMutableData *escPosData = [NSMutableData data];
    
    unsigned char header[] = {0x1D, 0x76, 0x30, 0x00};
    [escPosData appendBytes:header length:sizeof(header)];
    
    free(rawData);
    return escPosData;
}

RCT_EXPORT_METHOD(printBarcode:(NSString *)data
                  type:(NSString *)type
                  options:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    if (!isConnected || !writeCharacteristic) {
        reject(@"NOT_CONNECTED", @"Printer is not connected", nil);
        return;
    }
    
    NSData *barcodeCommands = [self generateBarcodeCommands:data type:type options:options];
    if (barcodeCommands.length == 0) {
        reject(@"INVALID_BARCODE", @"Failed to generate barcode commands", nil);
        return;
    }
    
    [connectedPeripheral writeValue:barcodeCommands
                  forCharacteristic:writeCharacteristic
                               type:CBCharacteristicWriteWithoutResponse];
    
    resolve(@YES);
}

RCT_EXPORT_METHOD(printQRCode:(NSString *)data
                  options:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    if (!isConnected || !writeCharacteristic) {
        reject(@"NOT_CONNECTED", @"Printer is not connected", nil);
        return;
    }
    
    NSData *qrCommands = [self generateQRCodeCommands:data options:options];
    if (qrCommands.length == 0) {
        reject(@"INVALID_QRCODE", @"Failed to generate QR code commands", nil);
        return;
    }
    
    [connectedPeripheral writeValue:qrCommands
                  forCharacteristic:writeCharacteristic
                               type:CBCharacteristicWriteWithoutResponse];
    
    resolve(@YES);
}

- (NSData *)generateBarcodeCommands:(NSString *)data type:(NSString *)type options:(NSDictionary *)options {
    NSMutableData *commands = [NSMutableData data];
    
    NSNumber *widthNum = options[@"width"] ?: @2;
    NSNumber *heightNum = options[@"height"] ?: @162;
    NSString *align = options[@"align"] ?: @"LEFT";
    NSString *textPosition = options[@"textPosition"] ?: @"BELOW";
    
    int width = MAX(2, MIN(6, [widthNum intValue]));
    int height = MAX(1, MIN(255, [heightNum intValue]));
    
    [commands appendData:[self getAlignmentCommand:align]];
    
    unsigned char widthCmd[] = {0x1D, 0x77, (unsigned char)width};
    [commands appendBytes:widthCmd length:sizeof(widthCmd)];
    
    unsigned char heightCmd[] = {0x1D, 0x68, (unsigned char)height};
    [commands appendBytes:heightCmd length:sizeof(heightCmd)];
    
    unsigned char textPos = [self getTextPosition:textPosition];
    unsigned char textPosCmd[] = {0x1D, 0x48, textPos};
    [commands appendBytes:textPosCmd length:sizeof(textPosCmd)];
    
    unsigned char barcodeType = [self getBarcodeType:type];
    if (barcodeType == 255) {
        return [NSData data];
    }
    
    NSData *dataBytes = [data dataUsingEncoding:NSUTF8StringEncoding];
    unsigned char barcodeHeader[] = {0x1D, 0x6B, barcodeType, (unsigned char)dataBytes.length};
    [commands appendBytes:barcodeHeader length:sizeof(barcodeHeader)];
    [commands appendData:dataBytes];
    
    unsigned char lineFeed[] = {0x0A};
    [commands appendBytes:lineFeed length:sizeof(lineFeed)];
    
    return commands;
}

- (NSData *)generateQRCodeCommands:(NSString *)data options:(NSDictionary *)options {
    NSMutableData *commands = [NSMutableData data];
    
    NSNumber *sizeNum = options[@"size"] ?: @6;
    NSString *align = options[@"align"] ?: @"LEFT";
    NSString *errorLevel = options[@"errorLevel"] ?: @"M";
    
    int size = MAX(1, MIN(16, [sizeNum intValue]));
    
    [commands appendData:[self getAlignmentCommand:align]];
    
    unsigned char modelCmd[] = {0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00};
    [commands appendBytes:modelCmd length:sizeof(modelCmd)];
    
    unsigned char sizeCmd[] = {0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, (unsigned char)size};
    [commands appendBytes:sizeCmd length:sizeof(sizeCmd)];
    
    unsigned char errorLevelByte = [self getErrorLevel:errorLevel];
    unsigned char errorCmd[] = {0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, errorLevelByte};
    [commands appendBytes:errorCmd length:sizeof(errorCmd)];
    
    NSData *dataBytes = [data dataUsingEncoding:NSUTF8StringEncoding];
    NSUInteger dataLength = dataBytes.length;
    
    unsigned char storeHeader[] = {
        0x1D, 0x28, 0x6B,
        (unsigned char)((dataLength + 3) & 0xFF),
        (unsigned char)(((dataLength + 3) >> 8) & 0xFF),
        0x31, 0x50, 0x30
    };
    [commands appendBytes:storeHeader length:sizeof(storeHeader)];
    [commands appendData:dataBytes];
    
    unsigned char printCmd[] = {0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30};
    [commands appendBytes:printCmd length:sizeof(printCmd)];
    
    unsigned char lineFeed[] = {0x0A};
    [commands appendBytes:lineFeed length:sizeof(lineFeed)];
    
    return commands;
}

- (NSData *)getAlignmentCommand:(NSString *)align {
    if ([align isEqualToString:@"CENTER"]) {
        unsigned char cmd[] = {0x1B, 0x61, 0x01};
        return [NSData dataWithBytes:cmd length:sizeof(cmd)];
    } else if ([align isEqualToString:@"RIGHT"]) {
        unsigned char cmd[] = {0x1B, 0x61, 0x02};
        return [NSData dataWithBytes:cmd length:sizeof(cmd)];
    } else {
        unsigned char cmd[] = {0x1B, 0x61, 0x00};
        return [NSData dataWithBytes:cmd length:sizeof(cmd)];
    }
}

- (unsigned char)getBarcodeType:(NSString *)type {
    NSDictionary *types = @{
        @"UPC_A": @0, @"UPC_E": @1, @"EAN13": @2, @"EAN8": @3,
        @"CODE39": @4, @"ITF": @5, @"CODABAR": @6,
        @"CODE93": @72, @"CODE128": @73
    };
    NSNumber *typeNum = types[type.uppercaseString];
    return typeNum ? [typeNum unsignedCharValue] : 255;
}

- (unsigned char)getTextPosition:(NSString *)position {
    if ([position isEqualToString:@"NONE"]) return 0;
    if ([position isEqualToString:@"ABOVE"]) return 1;
    if ([position isEqualToString:@"BELOW"]) return 2;
    if ([position isEqualToString:@"BOTH"]) return 3;
    return 2;
}

- (unsigned char)getErrorLevel:(NSString *)level {
    if ([level isEqualToString:@"L"]) return 48;
    if ([level isEqualToString:@"M"]) return 49;
    if ([level isEqualToString:@"Q"]) return 50;
    if ([level isEqualToString:@"H"]) return 51;
    return 49;
}

RCT_EXPORT_METHOD(cutPaper:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    resolve(@YES);
}

RCT_EXPORT_METHOD(openCashDrawer:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    resolve(@YES);
}

RCT_EXPORT_METHOD(printRaw:(NSArray *)data
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    resolve(@YES);
}

#pragma mark - CBPeripheralDelegate

- (void)peripheral:(CBPeripheral *)peripheral didDiscoverServices:(NSError *)error {
}

#ifdef RCT_NEW_ARCH_ENABLED
- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativePosPrinterSpecJSI>(params);
}
#endif

@end