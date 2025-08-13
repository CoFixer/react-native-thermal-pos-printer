import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-thermal-pos-printer' doesn't seem to be linked. Make sure: \n\n` +
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

// Enhanced interfaces with better type safety
export interface PrinterDevice {
  name: string;
  address: string;
  type: 'BLUETOOTH' | 'USB' | 'WIFI' | 'ETHERNET';
  connected: boolean;
  rssi?: number;
  batteryLevel?: number;
}

export interface PrintOptions {
  encoding?: 'UTF-8' | 'GBK' | 'GB2312' | 'BIG5';
  codepage?: number;
  width?: number;
  height?: number;
  beep?: boolean;
  cut?: boolean;
  tailingLine?: boolean;
  openCashBox?: boolean;
  copies?: number;
}

export interface TextOptions {
  align?: 'LEFT' | 'CENTER' | 'RIGHT';
  size?: 'SMALL' | 'NORMAL' | 'LARGE' | 'XLARGE' | number;
  bold?: boolean;
  underline?: boolean;
  fontType?: 'A' | 'B' | 'C';
  italic?: boolean;
  strikethrough?: boolean;
  doubleStrike?: boolean;
  invert?: boolean;
  rotate?: 0 | 90 | 180 | 270;
}

export interface ImageOptions {
  width?: number;
  height?: number;
  align?: 'LEFT' | 'CENTER' | 'RIGHT';
  threshold?: number;
  dithering?: boolean;
}

export interface BarcodeOptions {
  width?: number;
  height?: number;
  align?: 'LEFT' | 'CENTER' | 'RIGHT';
  textPosition?: 'NONE' | 'ABOVE' | 'BELOW' | 'BOTH';
  fontSize?: number;
  hri?: boolean;
}

export interface QRCodeOptions {
  size?: number;
  align?: 'LEFT' | 'CENTER' | 'RIGHT';
  errorLevel?: 'L' | 'M' | 'Q' | 'H';
  model?: 1 | 2;
}

export interface PrinterStatus {
  online: boolean;
  paperOut: boolean;
  coverOpen: boolean;
  cutterError: boolean;
  temperature: 'NORMAL' | 'HIGH';
  voltage: 'NORMAL' | 'LOW';
}

// Enhanced error types
export class PrinterError extends Error {
  code: string;
  
  constructor(code: string, message: string) {
    super(message);
    this.code = code;
    this.name = 'PrinterError';
  }
}

export class ReactNativePosPrinter {
  /**
   * Initialize the printer module
   */
  static async init(): Promise<boolean> {
    try {
      return await PosPrinter.init();
    } catch (error) {
      throw new PrinterError('INIT_FAILED', `Failed to initialize printer: ${error}`);
    }
  }

  /**
   * Get list of available printer devices
   */
  static async getDeviceList(): Promise<PrinterDevice[]> {
    try {
      return await PosPrinter.getDeviceList();
    } catch (error) {
      throw new PrinterError('DEVICE_LIST_FAILED', `Failed to get device list: ${error}`);
    }
  }

  /**
   * Connect to a printer device
   */
  static async connectPrinter(address: string, type: string): Promise<boolean> {
    if (!address || !type) {
      throw new PrinterError('INVALID_PARAMS', 'Address and type are required');
    }
    
    try {
      return await PosPrinter.connectPrinter(address, type);
    } catch (error) {
      throw new PrinterError('CONNECTION_FAILED', `Failed to connect to printer: ${error}`);
    }
  }

  /**
   * Disconnect from current printer
   */
  static async disconnectPrinter(): Promise<boolean> {
    try {
      return await PosPrinter.disconnectPrinter();
    } catch (error) {
      throw new PrinterError('DISCONNECT_FAILED', `Failed to disconnect: ${error}`);
    }
  }

  /**
   * Check if printer is connected
   */
  static async isConnected(): Promise<boolean> {
    try {
      return await PosPrinter.isConnected();
    } catch (error) {
      return false;
    }
  }

  /**
   * Print text with formatting options
   */
  static async printText(text: string, options: TextOptions = {}): Promise<boolean> {
    if (!text) {
      throw new PrinterError('INVALID_TEXT', 'Text cannot be empty');
    }
    
    try {
      return await PosPrinter.printText(text, options);
    } catch (error) {
      throw new PrinterError('PRINT_TEXT_FAILED', `Failed to print text: ${error}`);
    }
  }

  /**
   * Print image from base64 string
   */
  static async printImage(base64: string, options: ImageOptions = {}): Promise<boolean> {
    if (!base64) {
      throw new PrinterError('INVALID_IMAGE', 'Base64 image data is required');
    }
    
    try {
      return await PosPrinter.printImage(base64, options);
    } catch (error) {
      throw new PrinterError('PRINT_IMAGE_FAILED', `Failed to print image: ${error}`);
    }
  }

  /**
   * Print barcode
   */
  static async printBarcode(
    data: string, 
    type: string, 
    options: BarcodeOptions = {}
  ): Promise<boolean> {
    if (!data || !type) {
      throw new PrinterError('INVALID_BARCODE', 'Data and type are required');
    }
    
    try {
      return await PosPrinter.printBarcode(data, type, options);
    } catch (error) {
      throw new PrinterError('PRINT_BARCODE_FAILED', `Failed to print barcode: ${error}`);
    }
  }

  /**
   * Print QR code
   */
  static async printQRCode(data: string, options: QRCodeOptions = {}): Promise<boolean> {
    if (!data) {
      throw new PrinterError('INVALID_QRCODE', 'QR code data is required');
    }
    
    try {
      return await PosPrinter.printQRCode(data, options);
    } catch (error) {
      throw new PrinterError('PRINT_QRCODE_FAILED', `Failed to print QR code: ${error}`);
    }
  }

  /**
   * Print raw ESC/POS commands
   */
  static async printRaw(data: number[]): Promise<boolean> {
    if (!Array.isArray(data) || data.length === 0) {
      throw new PrinterError('INVALID_RAW_DATA', 'Raw data must be a non-empty array');
    }
    
    try {
      return await PosPrinter.printRaw(data);
    } catch (error) {
      throw new PrinterError('PRINT_RAW_FAILED', `Failed to print raw data: ${error}`);
    }
  }

  /**
   * Cut paper
   */
  static async cutPaper(): Promise<boolean> {
    try {
      return await PosPrinter.cutPaper();
    } catch (error) {
      throw new PrinterError('CUT_PAPER_FAILED', `Failed to cut paper: ${error}`);
    }
  }

  /**
   * Open cash drawer
   */
  static async openCashDrawer(): Promise<boolean> {
    try {
      return await PosPrinter.openCashDrawer();
    } catch (error) {
      throw new PrinterError('CASH_DRAWER_FAILED', `Failed to open cash drawer: ${error}`);
    }
  }

  /**
   * Print and cut paper
   */
  static async printAndCut(options: PrintOptions = {}): Promise<boolean> {
    try {
      return await PosPrinter.printAndCut(options);
    } catch (error) {
      throw new PrinterError('PRINT_AND_CUT_FAILED', `Failed to print and cut: ${error}`);
    }
  }

  /**
   * Get printer status
   */
  static async getPrinterStatus(): Promise<PrinterStatus> {
    try {
      const status = await PosPrinter.getPrinterStatus();
      return typeof status === 'string' ? JSON.parse(status) : status;
    } catch (error) {
      throw new PrinterError('STATUS_FAILED', `Failed to get printer status: ${error}`);
    }
  }

  /**
   * Set text encoding
   */
  static async setEncoding(encoding: string): Promise<boolean> {
    if (!encoding) {
      throw new PrinterError('INVALID_ENCODING', 'Encoding is required');
    }
    
    try {
      return await PosPrinter.setEncoding(encoding);
    } catch (error) {
      throw new PrinterError('SET_ENCODING_FAILED', `Failed to set encoding: ${error}`);
    }
  }

  /**
   * Print receipt with multiple items
   */
  static async printReceipt(items: {
    header?: string;
    items: Array<{ name: string; price: string; quantity?: number }>;
    total: string;
    footer?: string;
    qrCode?: string;
  }): Promise<boolean> {
    try {
      // Print header
      if (items.header) {
        await this.printText(items.header, { align: 'CENTER', bold: true, size: 'LARGE' });
        await this.printText('================================');
      }
      
      // Print items
      for (const item of items.items) {
        const quantity = item.quantity ? `${item.quantity}x ` : '';
        const line = `${quantity}${item.name.padEnd(20)}${item.price.padStart(8)}`;
        await this.printText(line);
      }
      
      await this.printText('================================');
      
      // Print total
      await this.printText(`TOTAL: ${items.total}`, { align: 'RIGHT', bold: true });
      
      // Print QR code if provided
      if (items.qrCode) {
        await this.printQRCode(items.qrCode, { align: 'CENTER', size: 6 });
      }
      
      // Print footer
      if (items.footer) {
        await this.printText(items.footer, { align: 'CENTER' });
      }
      
      // Cut paper
      await this.cutPaper();
      
      return true;
    } catch (error) {
      throw new PrinterError('PRINT_RECEIPT_FAILED', `Failed to print receipt: ${error}`);
    }
  }
}

// Export constants for barcode types
export const BarcodeTypes = {
  UPC_A: 'UPC_A',
  UPC_E: 'UPC_E',
  EAN13: 'EAN13',
  EAN8: 'EAN8',
  CODE39: 'CODE39',
  ITF: 'ITF',
  CODABAR: 'CODABAR',
  CODE93: 'CODE93',
  CODE128: 'CODE128',
} as const;

// Export ESC/POS commands for advanced users
export const ESCPOSCommands = {
  INIT: [0x1B, 0x40],
  CUT_PAPER: [0x1D, 0x56, 0x00],
  ALIGN_LEFT: [0x1B, 0x61, 0x00],
  ALIGN_CENTER: [0x1B, 0x61, 0x01],
  ALIGN_RIGHT: [0x1B, 0x61, 0x02],
  BOLD_ON: [0x1B, 0x45, 0x01],
  BOLD_OFF: [0x1B, 0x45, 0x00],
  UNDERLINE_ON: [0x1B, 0x2D, 0x01],
  UNDERLINE_OFF: [0x1B, 0x2D, 0x00],
  LINE_FEED: [0x0A],
  FORM_FEED: [0x0C],
} as const;

export default ReactNativePosPrinter;