import { PosPrinter } from "./native";
import { NativeModules, Platform } from 'react-native';

export class PosPrinterDebug {
  static log(message: string, ...args: any[]) {
    if (__DEV__) {
      console.log(`[PosPrinter] ${message}`, ...args);
    }
  }
  
  static error(message: string, ...args: any[]) {
    if (__DEV__) {
      console.error(`[PosPrinter] ${message}`, ...args);
    }
  }
  
  static checkModuleRegistration() {
    const moduleAvailable = !!NativeModules.PosPrinter;
    const availableMethods = moduleAvailable ? Object.keys(NativeModules.PosPrinter).filter(key => typeof NativeModules.PosPrinter[key] === 'function') : [];
    
    const debugInfo = {
      moduleAvailable,
      platform: Platform.OS,
      availableMethods,
      nativeModules: Object.keys(NativeModules)
    };
    
    console.log('Module Registration Debug:', debugInfo);
    
    if (!debugInfo.moduleAvailable) {
      console.error('❌ Native module not found!');
      console.log('Available native modules:', debugInfo.nativeModules);
      console.log('Platform:', debugInfo.platform);
    } else {
      console.log('✅ Native module is available');
      console.log('Available methods:', debugInfo.availableMethods);
    }
  }
}

export async function testNativeModule() {
  try {
    console.log('Testing native module availability...');
    const result = await PosPrinter.init({});
    console.log('Init result:', result);
    return true;
  } catch (error) {
    console.error('Native module test failed:', error);
    return false;
  }
}