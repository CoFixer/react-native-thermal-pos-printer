import { PosPrinter } from '../utils/native';
import {
  PrintOptions,
  TextOptions,
  ImageOptions,
  BarcodeOptions,
  QRCodeOptions,
  ThermalPrinterNativeDevice,
  ConnectionOptions
} from '../types/printer';
import { PrinterError, PrinterErrorCode } from '../types/errors';
import { BarcodeTypes } from '../constants/barcodes';
import { ESCPOSCommands } from '../constants/commands';
import { ThermalPrinterDevice } from './ThermalPrinterDevice';

export class ReactNativePosPrinter {
  private static currentDevice: ThermalPrinterDevice | null = null;

  private static async ensureConnected(): Promise<void> {
    const connected = await this.isConnected();
    if (!connected) {
      throw new PrinterError(
        PrinterErrorCode.NOT_CONNECTED,
        'No printer connected. Please connect a printer first.'
      );
    }
  }

  static async init(options?: PrintOptions): Promise<void> {
    try {
      if (!PosPrinter) {
        throw new Error('PosPrinter native module is not available');
      }
      
      await PosPrinter.init(options);
    } catch (error) {
      throw new PrinterError(
        PrinterErrorCode.INIT_FAILED,
        `Failed to initialize printer: ${error instanceof Error ? error.message : 'Unknown error'}`,
        error
      );
    }
  }

  static async getDeviceList(): Promise<ThermalPrinterDevice[]> {
    try {
      if (!PosPrinter) {
        throw new Error('PosPrinter native module is not available');
      }
      
      const devices = await PosPrinter.getDeviceList();
      return devices.map((device: any) => new ThermalPrinterDevice({
        name: device.name || 'Unknown Device',
        address: device.address,
        id: device.address,
        type: device.type?.toUpperCase() || 'BLUETOOTH',
        connected: device.connected || false,
        rssi: device.rssi,
        batteryLevel: device.batteryLevel,
        bondState: device.bondState,
        deviceClass: device.deviceClass,
        extra: device.extra
      }));
    } catch (error) {
      throw new PrinterError(
        PrinterErrorCode.DEVICE_LIST_FAILED,
        `Failed to get device list: ${error instanceof Error ? error.message : 'Unknown error'}`,
        error
      );
    }
  }

  static async connectPrinter(address: string, options?: ConnectionOptions): Promise<ThermalPrinterDevice> {
    try {
      const deviceType = options?.type || 'BLUETOOTH';
      await PosPrinter.connectPrinter(address, deviceType);
      
      const devices = await PosPrinter.getDeviceList();
      const device = devices.find((d: ThermalPrinterNativeDevice) => d.address === address);
      
      if (!device) {
        throw new PrinterError(
          PrinterErrorCode.DEVICE_NOT_FOUND,
          `Device with address ${address} not found`
        );
      }
  
      this.currentDevice = new ThermalPrinterDevice(device);
      return this.currentDevice;
    } catch (error) {
      if (error instanceof PrinterError) {
        throw error;
      }
      throw new PrinterError(
        PrinterErrorCode.CONNECTION_FAILED,
        `Failed to connect to printer ${address}`,
        error
      );
    }
  }

  static async disconnectPrinter(): Promise<void> {
    try {
      await PosPrinter.disconnectPrinter();
      this.currentDevice = null;
    } catch (error) {
      throw new PrinterError(
        PrinterErrorCode.DISCONNECTION_FAILED,
        'Failed to disconnect printer',
        error
      );
    }
  }

  static async isConnected(): Promise<boolean> {
    try {
      return await PosPrinter.isConnected();
    } catch (error) {
      return false;
    }
  }

  static async sendRawCommand(command: number[]): Promise<void> {
    try {
      await PosPrinter.sendRawCommand(command);
    } catch (error) {
      throw new PrinterError(
        PrinterErrorCode.PRINT_FAILED,
        'Failed to send raw command',
        error
      );
    }
  }

  static async initializePrinter(): Promise<void> {
    await this.sendRawCommand([...ESCPOSCommands.INIT]);
  }

  static async setAlignment(alignment: 'LEFT' | 'CENTER' | 'RIGHT'): Promise<void> {
    const commands = {
      LEFT: ESCPOSCommands.ALIGN_LEFT,
      CENTER: ESCPOSCommands.ALIGN_CENTER,
      RIGHT: ESCPOSCommands.ALIGN_RIGHT
    };
    await this.sendRawCommand([...commands[alignment]]);
  }

  static async setBold(enabled: boolean): Promise<void> {
    const command = enabled ? ESCPOSCommands.BOLD_ON : ESCPOSCommands.BOLD_OFF;
    await this.sendRawCommand([...command]);
  }

  static async setUnderline(enabled: boolean): Promise<void> {
    const command = enabled ? ESCPOSCommands.UNDERLINE_ON : ESCPOSCommands.UNDERLINE_OFF;
    await this.sendRawCommand([...command]);
  }

  static async feedLine(): Promise<void> {
    await this.sendRawCommand([...ESCPOSCommands.LINE_FEED]);
  }

  static async formFeed(): Promise<void> {
    await this.sendRawCommand([...ESCPOSCommands.FORM_FEED]);
  }

  static async newLine(lines: number = 1): Promise<void> {
    await this.ensureConnected();
    
    if (lines < 1) {
      throw new PrinterError(
        PrinterErrorCode.INVALID_PARAMETER,
        'Number of lines must be at least 1'
      );
    }

    if (lines > 10) {
      throw new PrinterError(
        PrinterErrorCode.INVALID_PARAMETER,
        'Number of lines cannot exceed 10 for performance reasons'
      );
    }

    try {
      for (let i = 0; i < lines; i++) {
        await this.feedLine();
      }
    } catch (error) {
      throw new PrinterError(
        PrinterErrorCode.PRINT_FAILED,
        `Failed to add new lines (${lines})`,
        error
      );
    }
  }

  static async printText(text: string, options?: TextOptions): Promise<void> {
    await this.ensureConnected();
    
    if (!text || text.trim().length === 0) {
      throw new PrinterError(
        PrinterErrorCode.INVALID_TEXT,
        'Text cannot be empty'
      );
    }

    try {
      await PosPrinter.printText(text, options);
    } catch (error) {
      throw new PrinterError(
        PrinterErrorCode.PRINT_FAILED,
        'Failed to print text',
        error
      );
    }
  }

  static async printImage(imageUri: string, options?: ImageOptions): Promise<void> {
    await this.ensureConnected();
    
    if (!imageUri) {
      throw new PrinterError(
        PrinterErrorCode.INVALID_PARAMETER,
        'Image URI cannot be empty'
      );
    }

    try {
      await PosPrinter.printImage(imageUri, options);
    } catch (error) {
      throw new PrinterError(
        PrinterErrorCode.PRINT_FAILED,
        'Failed to print image',
        error
      );
    }
  }

  static async cutPaper(): Promise<void> {
    await this.ensureConnected();
    
    try {
      await this.sendRawCommand([...ESCPOSCommands.CUT_PAPER]);
    } catch (error) {
      throw new PrinterError(
        PrinterErrorCode.PRINT_FAILED,
        'Failed to cut paper',
        error
      );
    }
  }

  static async printBarcode(data: string, type: keyof typeof BarcodeTypes, options?: BarcodeOptions): Promise<void> {
    if (!data) {
      throw new PrinterError(
        PrinterErrorCode.INVALID_PARAMETER,
        'Barcode data cannot be empty'
      );
    }

    try {
      await PosPrinter.printBarcode(data, type, options);
    } catch (error) {
      throw new PrinterError(
        PrinterErrorCode.PRINT_FAILED,
        'Failed to print barcode',
        error
      );
    }
  }

  static async printQRCode(data: string, options?: QRCodeOptions): Promise<void> {
    if (!data) {
      throw new PrinterError(
        PrinterErrorCode.INVALID_PARAMETER,
        'QR code data cannot be empty'
      );
    }

    try {
      await PosPrinter.printQRCode(data, options);
    } catch (error) {
      throw new PrinterError(
        PrinterErrorCode.PRINT_FAILED,
        'Failed to print QR code',
        error
      );
    }
  }

  static async openCashDrawer(): Promise<void> {
    try {
      await PosPrinter.openCashDrawer();
    } catch (error) {
      throw new PrinterError(
        PrinterErrorCode.HARDWARE_ERROR,
        'Failed to open cash drawer',
        error
      );
    }
  }

  static async printRaw(data: number[]): Promise<void> {
    if (!data || data.length === 0) {
      throw new PrinterError(
        PrinterErrorCode.INVALID_PARAMETER,
        'Raw data cannot be empty'
      );
    }

    try {
      await this.sendRawCommand(data);
    } catch (error) {
      throw new PrinterError(
        PrinterErrorCode.PRINT_FAILED,
        'Failed to print raw data',
        error
      );
    }
  }

  static getCurrentDevice(): ThermalPrinterDevice | null {
    return this.currentDevice;
  }
}