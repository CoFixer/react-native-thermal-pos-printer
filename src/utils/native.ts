import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-thermal-pos-printer' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n' +
  '- For Android: Check if the module is properly registered in MainApplication.java\n' +
  '- For iOS: Verify the module is included in the Podfile\n';

interface IPosPrinterNativeModule {
  init(options?: any): Promise<void>;
  getDeviceList(): Promise<any[]>;
  connectPrinter(address: string, options?: any): Promise<boolean>;
  connectBluetoothPrinter(address: string): Promise<boolean>;
  disconnectPrinter(): Promise<boolean>;
  isConnected(): Promise<boolean>;
  printText(text: string, options?: any): Promise<boolean>;
  printImage(base64: string, options?: any): Promise<boolean>;
  printBarcode(data: string, type: string, options?: any): Promise<boolean>;
  printQRCode(data: string, options?: any): Promise<boolean>;
  cutPaper(): Promise<boolean>;
  openCashDrawer(): Promise<boolean>;
  sendRawCommand(command: number[]): Promise<boolean>;
  printRaw(data: number[]): Promise<boolean>;
  getStatus(): Promise<any>;
}

class PosPrinterNativeModule implements IPosPrinterNativeModule {
  private nativeModule: any;
  private isAvailable: boolean;
  private debugMode: boolean;

  constructor(debugMode: boolean = __DEV__) {
    this.nativeModule = NativeModules.PosPrinter;
    this.isAvailable = !!this.nativeModule;
    this.debugMode = debugMode;
    
    if (!this.isAvailable && this.debugMode) {
      console.warn('PosPrinter native module not found. Available modules:', Object.keys(NativeModules));
    }
  }

  private createMethodProxy(methodName: string) {
    return (...args: any[]) => {
      if (!this.isAvailable) {
        throw new Error(LINKING_ERROR);
      }

      const method = this.nativeModule[methodName];
      if (typeof method !== 'function') {
        throw new Error(
          `Method '${methodName}' not found in PosPrinter native module. ` +
          `Available methods: ${Object.keys(this.nativeModule).filter(key => typeof this.nativeModule[key] === 'function').join(', ')}`
        );
      }

      try {
        return method.apply(this.nativeModule, args);
      } catch (error) {
        if (this.debugMode) {
          console.error(`Error calling PosPrinter.${methodName}:`, error);
        }
        throw error;
      }
    };
  }

  async init(options?: any): Promise<void> {
    return this.createMethodProxy('init')(options);
  }

  async getDeviceList(): Promise<any[]> {
    return this.createMethodProxy('getDeviceList')();
  }

  async connectPrinter(address: string, options?: any): Promise<boolean> {
    return this.createMethodProxy('connectPrinter')(address, options);
  }

  async connectBluetoothPrinter(address: string): Promise<boolean> {
    return this.createMethodProxy('connectBluetoothPrinter')(address);
  }

  async disconnectPrinter(): Promise<boolean> {
    return this.createMethodProxy('disconnectPrinter')();
  }

  async isConnected(): Promise<boolean> {
    return this.createMethodProxy('isConnected')();
  }

  async printText(text: string, options?: any): Promise<boolean> {
    return this.createMethodProxy('printText')(text, options);
  }

  async printImage(base64: string, options?: any): Promise<boolean> {
    return this.createMethodProxy('printImage')(base64, options);
  }

  async printBarcode(data: string, type: string, options?: any): Promise<boolean> {
    return this.createMethodProxy('printBarcode')(data, type, options);
  }

  async printQRCode(data: string, options?: any): Promise<boolean> {
    return this.createMethodProxy('printQRCode')(data, options);
  }

  async cutPaper(): Promise<boolean> {
    return this.createMethodProxy('cutPaper')();
  }

  async openCashDrawer(): Promise<boolean> {
    return this.createMethodProxy('openCashDrawer')();
  }

  async sendRawCommand(command: number[]): Promise<boolean> {
    return this.createMethodProxy('sendRawCommand')(command);
  }

  async printRaw(data: number[]): Promise<boolean> {
    return this.createMethodProxy('printRaw')(data);
  }

  async getStatus(): Promise<any> {
    return this.createMethodProxy('getStatus')();
  }

  isModuleAvailable(): boolean {
    return this.isAvailable;
  }

  getAvailableMethods(): string[] {
    if (!this.isAvailable) return [];
    return Object.keys(this.nativeModule).filter(key => typeof this.nativeModule[key] === 'function');
  }

  getDebugInfo() {
    return {
      moduleAvailable: this.isAvailable,
      platform: Platform.OS,
      availableMethods: this.getAvailableMethods(),
      nativeModules: Object.keys(NativeModules)
    };
  }
}

export const PosPrinter = new PosPrinterNativeModule();

export const PosPrinterDebug = {
  isAvailable: () => PosPrinter.isModuleAvailable(),
  getInfo: () => PosPrinter.getDebugInfo(),
  listMethods: () => PosPrinter.getAvailableMethods()
};