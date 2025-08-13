import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-pos-printer' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const PosPrinter = NativeModules.PosPrinter
  ? NativeModules.PosPrinter
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export interface PrinterDevice {
  name: string;
  address: string;
  type: 'BLUETOOTH' | 'USB' | 'WIFI' | 'ETHERNET';
  connected: boolean;
}

export interface PrintOptions {
  encoding?: string;
  codepage?: number;
  width?: number;
  height?: number;
  beep?: boolean;
  cut?: boolean;
  tailingLine?: boolean;
  openCashBox?: boolean;
}

export interface TextOptions {
  align?: 'LEFT' | 'CENTER' | 'RIGHT';
  size?: number; 
  bold?: boolean;
  underline?: boolean;
  fontType?: 'A' | 'B' | 'C';
  italic?: boolean;
  strikethrough?: boolean;
  doubleStrike?: boolean;
}

export interface ImageOptions {
  width?: number;
  height?: number;
  align?: 'LEFT' | 'CENTER' | 'RIGHT';
}

export interface BarcodeOptions {
  width?: number;
  height?: number;
  align?: 'LEFT' | 'CENTER' | 'RIGHT';
  textPosition?: 'NONE' | 'ABOVE' | 'BELOW' | 'BOTH';
  fontSize?: number;
}

export interface QRCodeOptions {
  size?: number;
  align?: 'LEFT' | 'CENTER' | 'RIGHT';
  errorLevel?: 'L' | 'M' | 'Q' | 'H';
}

export class ReactNativePosPrinter {
  static async init(): Promise<boolean> {
    return PosPrinter.init();
  }

  static async getDeviceList(): Promise<PrinterDevice[]> {
    return PosPrinter.getDeviceList();
  }

  static async connectPrinter(address: string, type: string): Promise<boolean> {
    return PosPrinter.connectPrinter(address, type);
  }

  static async disconnectPrinter(): Promise<boolean> {
    return PosPrinter.disconnectPrinter();
  }

  static async isConnected(): Promise<boolean> {
    return PosPrinter.isConnected();
  }

  static async printText(text: string, options?: TextOptions): Promise<boolean> {
    return PosPrinter.printText(text, options || {});
  }

  static async printImage(base64: string, options?: ImageOptions): Promise<boolean> {
    return PosPrinter.printImage(base64, options || {});
  }

  static async printBarcode(data: string, type: string, options?: BarcodeOptions): Promise<boolean> {
    return PosPrinter.printBarcode(data, type, options || {});
  }

  static async printQRCode(data: string, options?: QRCodeOptions): Promise<boolean> {
    return PosPrinter.printQRCode(data, options || {});
  }

  static async printRaw(data: number[]): Promise<boolean> {
    return PosPrinter.printRaw(data);
  }

  static async cutPaper(): Promise<boolean> {
    return PosPrinter.cutPaper();
  }

  static async openCashDrawer(): Promise<boolean> {
    return PosPrinter.openCashDrawer();
  }

  static async printAndCut(options?: PrintOptions): Promise<boolean> {
    return PosPrinter.printAndCut(options || {});
  }

  static async getPrinterStatus(): Promise<string> {
    return PosPrinter.getPrinterStatus();
  }

  static async setEncoding(encoding: string): Promise<boolean> {
    return PosPrinter.setEncoding(encoding);
  }
}

export default ReactNativePosPrinter;