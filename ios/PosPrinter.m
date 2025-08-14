#import "PosPrinter.h"
#import <React/RCTLog.h>
#import <React/RCTUtils.h>

@implementation PosPrinter

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(init:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    resolve(nil);
}

RCT_EXPORT_METHOD(getDeviceList:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    NSArray *devices = @[];
    resolve(devices);
}

RCT_EXPORT_METHOD(connectPrinter:(NSString *)address
                  type:(NSString *)type
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    resolve(@YES);
}

RCT_EXPORT_METHOD(disconnectPrinter:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    resolve(@YES);
}

RCT_EXPORT_METHOD(isConnected:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    resolve(@NO);
}

RCT_EXPORT_METHOD(printText:(NSString *)text
                  options:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    resolve(@YES);
}

RCT_EXPORT_METHOD(printImage:(NSString *)base64Image
                  options:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    resolve(@YES);
}

RCT_EXPORT_METHOD(printBarcode:(NSString *)data
                  type:(NSString *)type
                  options:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    resolve(@YES);
}

RCT_EXPORT_METHOD(printQRCode:(NSString *)data
                  options:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    resolve(@YES);
}

RCT_EXPORT_METHOD(cutPaper:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    resolve(@YES);
}

RCT_EXPORT_METHOD(openCashDrawer:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    resolve(@YES);
}

RCT_EXPORT_METHOD(sendRawCommand:(NSArray *)command
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    resolve(@YES);
}

RCT_EXPORT_METHOD(getStatus:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    NSDictionary *status = @{@"status": @"ready"};
    resolve(status);
}

@end