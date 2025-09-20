#import <Foundation/Foundation.h>
#if __has_include(<React/RCTBridgeModule.h>)
#import <React/RCTBridgeModule.h>
#elif __has_include("RCTBridgeModule.h")
#import "RCTBridgeModule.h"
#elif __has_include("React/RCTBridgeModule.h")
#import "React/RCTBridgeModule.h"
#else
@protocol RCTBridgeModule <NSObject>
@end
#endif

#import <CoreBluetooth/CoreBluetooth.h>
#import <UIKit/UIKit.h>

@interface PosPrinter : NSObject <RCTBridgeModule, CBCentralManagerDelegate, CBPeripheralDelegate>

- (NSData *)printTextAsBitmap:(NSString *)text
                      fontSize:(CGFloat)fontSize
                    fontFamily:(NSString *)fontFamily
                        isBold:(BOOL)isBold
                      isItalic:(BOOL)isItalic
                   isUnderline:(BOOL)isUnderline
                   doubleWidth:(BOOL)doubleWidth
                  doubleHeight:(BOOL)doubleHeight
                 letterSpacing:(CGFloat)letterSpacing
                   lineSpacing:(CGFloat)lineSpacing
                         align:(NSString *)align;

- (BOOL)writeDataToPrinter:(NSData *)data;
- (NSData *)convertImageToRaster:(UIImage *)image;

@end