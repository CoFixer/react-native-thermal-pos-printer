#import "PosPrinter.h"
#import <React/RCTLog.h>
#import <React/RCTUtils.h>
#import <CoreBluetooth/CoreBluetooth.h>
#import <ExternalAccessory/ExternalAccessory.h>
#import <CoreText/CoreText.h>
#import <CoreGraphics/CoreGraphics.h>
#include <unistd.h>

@interface PosPrinter () <CBCentralManagerDelegate, CBPeripheralDelegate>
@property (nonatomic, strong) CBCentralManager *centralManager;
@property (nonatomic, strong) CBPeripheral *connectedPeripheral;
@property (nonatomic, strong) CBCharacteristic *writeCharacteristic;
@property (nonatomic, assign) BOOL isConnected;
@property (nonatomic, strong) NSMutableData *printerBuffer;
@property (nonatomic, strong) id printerService;
@end

@implementation PosPrinter

RCT_EXPORT_MODULE()

+ (NSDictionary *)constantsToExport {
    return @{
        @"Events": @{
            @"DEVICE_CONNECTED": @"DEVICE_CONNECTED",
            @"DEVICE_DISCONNECTED": @"DEVICE_DISCONNECTED",
            @"DEVICE_CONNECTION_LOST": @"DEVICE_CONNECTION_LOST",
            @"PRINT_STATUS": @"PRINT_STATUS"
        },
        @"Errors": @{
            @"BLUETOOTH_NOT_AVAILABLE": @"BLUETOOTH_NOT_AVAILABLE",
            @"BLUETOOTH_DISABLED": @"BLUETOOTH_DISABLED",
            @"NOT_CONNECTED": @"NOT_CONNECTED",
            @"CONNECTION_FAILED": @"CONNECTION_FAILED",
            @"UNSUPPORTED_TYPE": @"UNSUPPORTED_TYPE",
            @"PRINT_FAILED": @"PRINT_FAILED"
        }
    };
}

- (instancetype)init {
    self = [super init];
    if (self) {
        _isConnected = NO;
        _printerBuffer = [[NSMutableData alloc] init];
    }
    return self;
}

RCT_EXPORT_METHOD(init:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    self.centralManager = [[CBCentralManager alloc] initWithDelegate:self queue:nil];
    resolve(nil);
}

RCT_EXPORT_METHOD(getDeviceList:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    if (self.centralManager.state != CBManagerStatePoweredOn) {
        reject(@"BLUETOOTH_DISABLED", @"Bluetooth is not enabled", nil);
        return;
    }
    
    NSMutableArray *devices = [[NSMutableArray alloc] init];
    NSArray *connectedPeripherals = [self.centralManager retrieveConnectedPeripheralsWithServices:@[[CBUUID UUIDWithString:@"18F0"]]];
    
    for (CBPeripheral *peripheral in connectedPeripherals) {
        [devices addObject:@{
            @"name": peripheral.name ?: @"Unknown Device",
            @"address": peripheral.identifier.UUIDString
        }];
    }
    
    resolve(devices);
}

RCT_EXPORT_METHOD(connectPrinter:(NSString *)address
                  type:(NSString *)type
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    if ([type.lowercaseString isEqualToString:@"bluetooth"]) {
        NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:address];
        NSArray *peripherals = [self.centralManager retrievePeripheralsWithIdentifiers:@[uuid]];
        
        if (peripherals.count > 0) {
            self.connectedPeripheral = peripherals.firstObject;
            self.connectedPeripheral.delegate = self;
            [self.centralManager connectPeripheral:self.connectedPeripheral options:nil];
            resolve(@YES);
        } else {
            reject(@"DEVICE_NOT_FOUND", @"Device not found", nil);
        }
    } else {
        reject(@"UNSUPPORTED_TYPE", @"Unsupported printer type", nil);
    }
}

RCT_EXPORT_METHOD(disconnectPrinter:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    if (self.connectedPeripheral) {
        [self.centralManager cancelPeripheralConnection:self.connectedPeripheral];
    }
    self.isConnected = NO;
    resolve(@YES);
}

RCT_EXPORT_METHOD(isConnected:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    resolve(@(self.isConnected));
}

RCT_EXPORT_METHOD(printText:(NSString *)text
                  options:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    if (!self.isConnected) {
        reject(@"NOT_CONNECTED", @"Printer not connected", nil);
        return;
    }
    
    if (text.length == 0) {
        reject(@"UNSUPPORTED_TYPE", @"Text cannot be empty", nil);
        return;
    }
    
    NSMutableData *commandData = [[NSMutableData alloc] init];
    BOOL usedBitmapRendering = NO;
    NSString *alignValue = options[@"align"] ? [options[@"align"] lowercaseString] : @"left";
    
    if (options) {
        NSDictionary *formattingResult = [self applyTextFormatting:options commandData:commandData];
        
        BOOL needsBitmapForFont = options[@"fontType"] && ![options[@"fontType"] isEqualToString:@"A"];
        BOOL needsBitmapForStyling = (options[@"bold"] && [options[@"bold"] boolValue]) ||
                                    (options[@"italic"] && [options[@"italic"] boolValue]) ||
                                    (options[@"underline"] && [options[@"underline"] boolValue]);
        BOOL needsBitmapForSize = options[@"size"] && [options[@"size"] intValue] > 0;
        BOOL needsBitmapForAlignment = ![alignValue isEqualToString:@"left"];
        
        if ([formattingResult[@"shouldUseBitmapRendering"] boolValue] || needsBitmapForFont || needsBitmapForStyling || needsBitmapForSize || needsBitmapForAlignment) {
            usedBitmapRendering = YES;
            
            NSString *fontFamily = @"Helvetica";
            if (options[@"fontType"]) {
                NSString *fontType = [options[@"fontType"] uppercaseString];
                if ([fontType isEqualToString:@"A"]) fontFamily = @"Courier";
                else if ([fontType isEqualToString:@"B"]) fontFamily = @"Helvetica";
                else if ([fontType isEqualToString:@"C"]) fontFamily = @"Times";
            }
            
            BOOL isBold = options[@"bold"] ? [options[@"bold"] boolValue] : NO;
            BOOL isItalic = options[@"italic"] ? [options[@"italic"] boolValue] : NO;
            BOOL isUnderline = options[@"underline"] ? [options[@"underline"] boolValue] : NO;
            BOOL doubleWidth = options[@"doubleWidth"] ? [options[@"doubleWidth"] boolValue] : NO;
            BOOL doubleHeight = options[@"doubleHeight"] ? [options[@"doubleHeight"] boolValue] : NO;
            
            CGFloat fontSize = formattingResult[@"fontSizePt"] ? [formattingResult[@"fontSizePt"] floatValue] : 12.0f;
            CGFloat letterSpacing = formattingResult[@"letterSpacing"] ? [formattingResult[@"letterSpacing"] floatValue] : 1.0f;
            
            NSArray *lines = [self breakTextIntoLinesWithWordBreaking:text maxLineWidth:32];
            
            for (NSString *line in lines) {
                if (line.length > 0) {
                    NSData *bitmapData = [self printTextAsBitmap:line
                                                        fontSize:fontSize
                                                      fontFamily:fontFamily
                                                          isBold:isBold
                                                        isItalic:isItalic
                                                     isUnderline:isUnderline
                                                     doubleWidth:doubleWidth
                                                    doubleHeight:doubleHeight
                                                   letterSpacing:letterSpacing
                                                           align:alignValue];
                    if (bitmapData) {
                        // write each bitmap directly so peripheral can process in chunks
                        [self writeDataToPrinter:bitmapData];
                    }
                }
            }
        } else {
            [commandData appendBytes:"\x1B\x61\x00" length:3]; 
        }
    } else {
        [commandData appendBytes:"\x1B\x61\x00" length:3]; 
    }

    BOOL (^containsBangla)(NSString *) = ^BOOL(NSString *s) {
        for (NSUInteger i = 0; i < s.length; i++) {
            unichar ch = [s characterAtIndex:i];
            if (ch >= 0x0980 && ch <= 0x09FF) return YES;
        }
        return NO;
    };

    if (!usedBitmapRendering && containsBangla(text)) {
        usedBitmapRendering = YES;
        NSArray *lines = [self breakTextIntoLinesWithWordBreaking:text maxLineWidth:32];
        for (NSString *line in lines) {
            if (line.length > 0) {
                                NSData *bitmapData = [self printTextAsBitmap:line
                                    fontSize:12.0
                                    fontFamily:@"Bangla Sangam MN"
                                    isBold:NO
                                    isItalic:NO
                                    isUnderline:NO
                                    doubleWidth:NO
                                    doubleHeight:NO
                                    letterSpacing:1.0
                                    align:alignValue];
                                if (bitmapData) {
                                        [self writeDataToPrinter:bitmapData];
                                }
            }
        }
    }
    
    if (!usedBitmapRendering) {
        NSArray *lines = [self breakTextIntoLines:text maxLineWidth:32];
        for (NSString *line in lines) {
            NSString *lineWithNewline = [NSString stringWithFormat:@"%@\n", line];
            NSData *textData = [lineWithNewline dataUsingEncoding:NSUTF8StringEncoding];
            [commandData appendData:textData];
        }
    }
    
    [self writeDataToPrinter:commandData];
    resolve(@YES);
}

- (NSDictionary *)applyTextFormatting:(NSDictionary *)options commandData:(NSMutableData *)commandData {
    BOOL shouldUseBitmapRendering = NO;
    NSNumber *fontSizePt = @12;
    NSNumber *letterSpacing = @1.0f;
    
    if (options[@"align"]) {
        NSString *align = [options[@"align"] lowercaseString];
        if ([align isEqualToString:@"center"]) {
            [commandData appendBytes:"\x1B\x61\x01" length:3];
        } else if ([align isEqualToString:@"right"]) {
            [commandData appendBytes:"\x1B\x61\x02" length:3];
        } else {
            [commandData appendBytes:"\x1B\x61\x00" length:3];
        }
    }
    
    if (options[@"bold"] && [options[@"bold"] boolValue]) {
        [commandData appendBytes:"\x1B\x45\x01" length:3];
        shouldUseBitmapRendering = YES;
    }
    
    if (options[@"underline"] && [options[@"underline"] boolValue]) {
        [commandData appendBytes:"\x1B\x2D\x01" length:3];
        shouldUseBitmapRendering = YES;
    }
    
    if (options[@"size"]) {
        fontSizePt = @([options[@"size"] intValue]);
        shouldUseBitmapRendering = YES;
    }
    
    if (options[@"letterSpacing"]) {
        letterSpacing = @([options[@"letterSpacing"] floatValue]);
        shouldUseBitmapRendering = YES;
    }
    
    return @{
        @"shouldUseBitmapRendering": @(shouldUseBitmapRendering),
        @"fontSizePt": fontSizePt,
        @"letterSpacing": letterSpacing
    };
}

- (NSArray *)breakTextIntoLinesWithWordBreaking:(NSString *)text maxLineWidth:(NSInteger)maxLineWidth {
    NSMutableArray *lines = [[NSMutableArray alloc] init];
    NSArray *paragraphs = [text componentsSeparatedByString:@"\n"];
    
    for (NSString *paragraph in paragraphs) {
        if (paragraph.length <= maxLineWidth) {
            [lines addObject:paragraph];
        } else {
            NSArray *words = [paragraph componentsSeparatedByString:@" "];
            NSMutableString *currentLine = [[NSMutableString alloc] init];
            
            for (NSString *word in words) {
                NSString *testLine = currentLine.length > 0 ? [NSString stringWithFormat:@"%@ %@", currentLine, word] : word;
                
                if (testLine.length <= maxLineWidth) {
                    [currentLine setString:testLine];
                } else {
                    if (currentLine.length > 0) {
                        [lines addObject:[currentLine copy]];
                        [currentLine setString:word];
                    } else {
                        [currentLine setString:word];
                    }
                }
            }
            
            if (currentLine.length > 0) {
                [lines addObject:[currentLine copy]];
            }
        }
    }
    
    return lines;
}

- (NSArray *)breakTextIntoLines:(NSString *)text maxLineWidth:(NSInteger)maxLineWidth {
    NSMutableArray *lines = [[NSMutableArray alloc] init];
    NSArray *paragraphs = [text componentsSeparatedByString:@"\n"];
    
    for (NSString *paragraph in paragraphs) {
        for (NSInteger i = 0; i < paragraph.length; i += maxLineWidth) {
            NSInteger length = MIN(maxLineWidth, paragraph.length - i);
            NSString *line = [paragraph substringWithRange:NSMakeRange(i, length)];
            [lines addObject:line];
        }
    }
    
    return lines;
}

- (NSData *)printTextAsBitmap:(NSString *)text
                     fontSize:(CGFloat)fontSize
                   fontFamily:(NSString *)fontFamily
                       isBold:(BOOL)isBold
                     isItalic:(BOOL)isItalic
                  isUnderline:(BOOL)isUnderline
                  doubleWidth:(BOOL)doubleWidth
                 doubleHeight:(BOOL)doubleHeight
                letterSpacing:(CGFloat)letterSpacing
                        align:(NSString *)align {
    
    NSString *fontName = fontFamily;
    if (isBold && isItalic) {
        fontName = [fontFamily stringByAppendingString:@"-BoldItalic"];
    } else if (isBold) {
        fontName = [fontFamily stringByAppendingString:@"-Bold"];
    } else if (isItalic) {
        fontName = [fontFamily stringByAppendingString:@"-Italic"];
    }
    
    UIFont *font = [UIFont fontWithName:fontName size:fontSize] ?: [UIFont systemFontOfSize:fontSize];
    
    NSDictionary *attributes = @{NSFontAttributeName: font};
    CGSize textSize = [text sizeWithAttributes:attributes];
    
    if (doubleWidth) textSize.width *= 2;
    if (doubleHeight) textSize.height *= 2;
    
    CGFloat printerWidth = 384;
    CGFloat bitmapWidth = MIN(textSize.width + 20, printerWidth);
    CGFloat bitmapHeight = textSize.height + 10;
    
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(bitmapWidth, bitmapHeight), NO, 1.0);
    CGContextRef context = UIGraphicsGetCurrentContext();
    
    CGContextSetFillColorWithColor(context, [UIColor whiteColor].CGColor);
    CGContextFillRect(context, CGRectMake(0, 0, bitmapWidth, bitmapHeight));
    
    CGFloat x = 5;
    if ([align isEqualToString:@"center"]) {
        x = (bitmapWidth - textSize.width) / 2;
    } else if ([align isEqualToString:@"right"]) {
        x = bitmapWidth - textSize.width - 5;
    }
    
    CGContextSetFillColorWithColor(context, [UIColor blackColor].CGColor);
    [text drawAtPoint:CGPointMake(x, 5) withAttributes:attributes];
    
    if (isUnderline) {
        CGContextSetStrokeColorWithColor(context, [UIColor blackColor].CGColor);
        CGContextSetLineWidth(context, 1.0);
        CGContextMoveToPoint(context, x, textSize.height + 6);
        CGContextAddLineToPoint(context, x + textSize.width, textSize.height + 6);
        CGContextStrokePath(context);
    }
    
    UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    return [self convertImageToRaster:image];
}

RCT_EXPORT_METHOD(printImage:(NSString *)base64Image
                  options:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    if (!self.isConnected) {
        reject(@"NOT_CONNECTED", @"Printer not connected", nil);
        return;
    }
    
    NSData *imageData = [[NSData alloc] initWithBase64EncodedString:base64Image options:0];
    UIImage *image = [UIImage imageWithData:imageData];
    
    if (!image) {
        reject(@"INVALID_IMAGE", @"Invalid image data", nil);
        return;
    }
    
    NSMutableData *alignCmd = [[NSMutableData alloc] init];

    if (options[@"align"]) {
        NSString *align = [options[@"align"] lowercaseString];
        if ([align isEqualToString:@"center"]) {
            [alignCmd appendBytes:"\x1B\x61\x01" length:3];
        } else if ([align isEqualToString:@"right"]) {
            [alignCmd appendBytes:"\x1B\x61\x02" length:3];
        } else {
            [alignCmd appendBytes:"\x1B\x61\x00" length:3];
        }
    }

    // send alignment first
    if (alignCmd.length > 0) {
        [self writeDataToPrinter:alignCmd];
    }

    UIImage *processedImage = [self convertToWhiteBackground:image];
    NSData *rasterData = [self convertImageToRaster:processedImage];

    // stream raster data in chunks to avoid overflowing BLE buffers
    NSUInteger total = rasterData.length;
    NSUInteger offset = 0;
    const NSUInteger CHUNK_SIZE = 1024;
    while (offset < total) {
        NSUInteger chunkLen = MIN(CHUNK_SIZE, total - offset);
        NSData *chunk = [rasterData subdataWithRange:NSMakeRange(offset, chunkLen)];
        [self writeDataToPrinter:chunk];
        offset += chunkLen;
    }

    // reset alignment to left after image
    uint8_t resetAlign[] = {0x1B, 0x61, 0x00};
    NSData *reset = [NSData dataWithBytes:resetAlign length:3];
    [self writeDataToPrinter:reset];
    resolve(@YES);
}

- (UIImage *)convertToWhiteBackground:(UIImage *)image {
    UIGraphicsBeginImageContextWithOptions(image.size, NO, image.scale);
    CGContextRef context = UIGraphicsGetCurrentContext();
    
    CGContextSetFillColorWithColor(context, [UIColor whiteColor].CGColor);
    CGContextFillRect(context, CGRectMake(0, 0, image.size.width, image.size.height));
    
    [image drawAtPoint:CGPointZero];
    
    UIImage *result = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    return result;
}

RCT_EXPORT_METHOD(printBarcode:(NSString *)data
                  type:(NSString *)type
                  options:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    if (!self.isConnected) {
        reject(@"NOT_CONNECTED", @"Printer not connected", nil);
        return;
    }
    
    NSMutableData *commandData = [[NSMutableData alloc] init];
    
    if (options[@"align"]) {
        NSString *align = [options[@"align"] lowercaseString];
        if ([align isEqualToString:@"center"]) {
            [commandData appendBytes:"\x1B\x61\x01" length:3];
        } else if ([align isEqualToString:@"right"]) {
            [commandData appendBytes:"\x1B\x61\x02" length:3];
        } else {
            [commandData appendBytes:"\x1B\x61\x00" length:3];
        }
    }
    
    int height = options[@"height"] ? [options[@"height"] intValue] : 60;
    int width = options[@"width"] ? [options[@"width"] intValue] : 2;
    
    uint8_t barcodeType = 8;
    if ([type.uppercaseString isEqualToString:@"CODE128"]) barcodeType = 8;
    else if ([type.uppercaseString isEqualToString:@"CODE39"]) barcodeType = 4;
    else if ([type.uppercaseString isEqualToString:@"EAN13"]) barcodeType = 2;
    else if ([type.uppercaseString isEqualToString:@"EAN8"]) barcodeType = 3;
    
    uint8_t heightCmd[] = {0x1D, 0x68, (uint8_t)height};
    uint8_t widthCmd[] = {0x1D, 0x77, (uint8_t)width};
    uint8_t hriCmd[] = {0x1D, 0x48, 0x02};
    
    [commandData appendBytes:heightCmd length:3];
    [commandData appendBytes:widthCmd length:3];
    [commandData appendBytes:hriCmd length:3];
    
    uint8_t printCmd[] = {0x1D, 0x6B, barcodeType};
    [commandData appendBytes:printCmd length:3];
    
    NSData *barcodeData = [data dataUsingEncoding:NSUTF8StringEncoding];
    [commandData appendData:barcodeData];
    [commandData appendBytes:"\0" length:1];
    
    [self writeDataToPrinter:commandData];
    resolve(@YES);
}

RCT_EXPORT_METHOD(printQRCode:(NSString *)data
                  options:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    if (!self.isConnected) {
        reject(@"NOT_CONNECTED", @"Printer not connected", nil);
        return;
    }
    
    NSMutableData *commandData = [[NSMutableData alloc] init];
    
    if (options[@"align"]) {
        NSString *align = [options[@"align"] lowercaseString];
        if ([align isEqualToString:@"center"]) {
            [commandData appendBytes:"\x1B\x61\x01" length:3];
        } else if ([align isEqualToString:@"right"]) {
            [commandData appendBytes:"\x1B\x61\x02" length:3];
        } else {
            [commandData appendBytes:"\x1B\x61\x00" length:3];
        }
    }
    
    int size = options[@"size"] ? [options[@"size"] intValue] : 5;
    
    uint8_t modelCmd[] = {0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00};
    uint8_t sizeCmd[] = {0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, (uint8_t)size};
    uint8_t errorCmd[] = {0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x31};
    
    [commandData appendBytes:modelCmd length:9];
    [commandData appendBytes:sizeCmd length:8];
    [commandData appendBytes:errorCmd length:8];
    
    NSData *qrData = [data dataUsingEncoding:NSUTF8StringEncoding];
    NSInteger dataLength = qrData.length + 3;
    
    uint8_t storeCmd[] = {0x1D, 0x28, 0x6B, (uint8_t)(dataLength & 0xFF), (uint8_t)((dataLength >> 8) & 0xFF), 0x31, 0x50, 0x30};
    [commandData appendBytes:storeCmd length:8];
    [commandData appendData:qrData];
    
    uint8_t printCmd[] = {0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30};
    [commandData appendBytes:printCmd length:8];
    
    [self writeDataToPrinter:commandData];
    resolve(@YES);
}

RCT_EXPORT_METHOD(cutPaper:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    if (!self.isConnected) {
        reject(@"NOT_CONNECTED", @"Printer not connected", nil);
        return;
    }
    
    uint8_t cutCmd[] = {0x1D, 0x56, 0x00};
    NSData *commandData = [NSData dataWithBytes:cutCmd length:3];
    [self writeDataToPrinter:commandData];
    resolve(@YES);
}

RCT_EXPORT_METHOD(openCashDrawer:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    if (!self.isConnected) {
        reject(@"NOT_CONNECTED", @"Printer not connected", nil);
        return;
    }
    
    uint8_t drawerCmd[] = {0x1B, 0x70, 0x00, 0x19, 0xFA};
    NSData *commandData = [NSData dataWithBytes:drawerCmd length:5];
    [self writeDataToPrinter:commandData];
    resolve(@YES);
}

RCT_EXPORT_METHOD(sendRawCommand:(NSArray *)command
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    if (!self.isConnected) {
        reject(@"NOT_CONNECTED", @"Printer not connected", nil);
        return;
    }
    
    NSMutableData *commandData = [[NSMutableData alloc] init];
    for (NSNumber *byte in command) {
        uint8_t byteValue = [byte unsignedCharValue];
        [commandData appendBytes:&byteValue length:1];
    }
    
    [self writeDataToPrinter:commandData];
    resolve(@YES);
}

RCT_EXPORT_METHOD(getStatus:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    NSDictionary *status = @{
        @"connected": @(self.isConnected),
        @"platform": @"ios"
    };
    resolve(status);
}

RCT_EXPORT_METHOD(enterPrinterBuffer:(BOOL)clear
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    if (clear) {
        [self.printerBuffer setLength:0];
    }
    resolve(@YES);
}

RCT_EXPORT_METHOD(exitPrinterBuffer:(BOOL)commit
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    if (commit && self.printerBuffer.length > 0) {
        [self writeDataToPrinter:self.printerBuffer];
    }
    [self.printerBuffer setLength:0];
    resolve(@YES);
}

RCT_EXPORT_METHOD(commitPrinterBuffer:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    if (self.printerBuffer.length > 0) {
        [self writeDataToPrinter:self.printerBuffer];
        [self.printerBuffer setLength:0];
    }
    resolve(@YES);
}

RCT_EXPORT_METHOD(resetPrinter:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    if (!self.isConnected) {
        reject(@"NOT_CONNECTED", @"Printer not connected", nil);
        return;
    }
    
    uint8_t initCmd[] = {0x1B, 0x40};
    NSData *commandData = [NSData dataWithBytes:initCmd length:2];
    [self writeDataToPrinter:commandData];
    resolve(@YES);
}

- (void)writeDataToPrinter:(NSData *)data {
    if (self.writeCharacteristic && self.connectedPeripheral) {
    [self.connectedPeripheral writeValue:data forCharacteristic:self.writeCharacteristic type:CBCharacteristicWriteWithoutResponse];
    // give peripheral a short time to process buffer to avoid trailing corruption
    usleep(50000); // 50ms
    }
}

- (NSData *)convertImageToRaster:(UIImage *)image {
    CGFloat width = 384;
    CGFloat height = image.size.height * (width / image.size.width);
    
    UIGraphicsBeginImageContext(CGSizeMake(width, height));
    [image drawInRect:CGRectMake(0, 0, width, height)];
    UIImage *resizedImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    CGImageRef cgImage = resizedImage.CGImage;
    NSUInteger imageWidth = CGImageGetWidth(cgImage);
    NSUInteger imageHeight = CGImageGetHeight(cgImage);
    
    NSUInteger bytesPerRow = (imageWidth + 7) / 8;
    NSMutableData *rasterData = [[NSMutableData alloc] init];
    
    uint8_t header[] = {0x1D, 0x76, 0x30, 0x00, (uint8_t)(bytesPerRow & 0xFF), (uint8_t)((bytesPerRow >> 8) & 0xFF), (uint8_t)(imageHeight & 0xFF), (uint8_t)((imageHeight >> 8) & 0xFF)};
    [rasterData appendBytes:header length:8];
    
    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceGray();
    unsigned char *bitmap = (unsigned char *)calloc(imageWidth * imageHeight, sizeof(unsigned char));
    CGContextRef context = CGBitmapContextCreate(bitmap, imageWidth, imageHeight, 8, imageWidth, colorSpace, kCGImageAlphaNone);
    
    CGContextDrawImage(context, CGRectMake(0, 0, imageWidth, imageHeight), cgImage);
    
    for (NSUInteger y = 0; y < imageHeight; y++) {
        for (NSUInteger x = 0; x < bytesPerRow; x++) {
            uint8_t byte = 0;
            for (int bit = 0; bit < 8; bit++) {
                NSUInteger pixelX = x * 8 + bit;
                if (pixelX < imageWidth) {
                    unsigned char pixel = bitmap[y * imageWidth + pixelX];
                    if (pixel < 128) {
                        byte |= (1 << (7 - bit));
                    }
                }
            }
            [rasterData appendBytes:&byte length:1];
        }
    }
    
    free(bitmap);
    CGContextRelease(context);
    CGColorSpaceRelease(colorSpace);
    
    return rasterData;
}

#pragma mark - CBCentralManagerDelegate

- (void)centralManagerDidUpdateState:(CBCentralManager *)central {
    
}

- (void)centralManager:(CBCentralManager *)central didConnectPeripheral:(CBPeripheral *)peripheral {
    self.isConnected = YES;
    [peripheral discoverServices:nil];
}

- (void)centralManager:(CBCentralManager *)central didDisconnectPeripheral:(CBPeripheral *)peripheral error:(NSError *)error {
    self.isConnected = NO;
    self.connectedPeripheral = nil;
    self.writeCharacteristic = nil;
}

#pragma mark - CBPeripheralDelegate

- (void)peripheral:(CBPeripheral *)peripheral didDiscoverServices:(NSError *)error {
    for (CBService *service in peripheral.services) {
        [peripheral discoverCharacteristics:nil forService:service];
    }
}

- (void)peripheral:(CBPeripheral *)peripheral didDiscoverCharacteristicsForService:(CBService *)service error:(NSError *)error {
    for (CBCharacteristic *characteristic in service.characteristics) {
        if (characteristic.properties & CBCharacteristicPropertyWrite || characteristic.properties & CBCharacteristicPropertyWriteWithoutResponse) {
            self.writeCharacteristic = characteristic;
            break;
        }
    }
}

@end