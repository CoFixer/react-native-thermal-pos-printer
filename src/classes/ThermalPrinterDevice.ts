import { PosPrinter } from '../utils/native';
import {
  TextOptions,
  ImageOptions,
  BarcodeOptions,
  QRCodeOptions,
  PrinterStatus,
  ThermalPrinterNativeDevice,
  ConnectionOptions
} from '../types/printer';
import { PrinterError, PrinterErrorCode } from '../types/errors';
import { PrinterEvent } from '../types/events';
import { BarcodeTypes } from '../constants/barcodes';
import { ESCPOSCommands } from '../constants/commands';
import { PrinterEventManager } from './PrinterEventManager';

export class ThermalPrinterDevice {
  private device: ThermalPrinterNativeDevice;
  private connected: boolean = false;

  constructor(device: ThermalPrinterNativeDevice) {
    this.device = device;
  }

  async connect(options?: ConnectionOptions): Promise<void> {
    try {
      await PosPrinter.connectPrinter(this.device.address, options);
      this.connected = true;
      PrinterEventManager.emit(PrinterEvent.DEVICE_CONNECTED, { device: this.device });
    } catch (error) {
      const printerError = new PrinterError(
        PrinterErrorCode.CONNECTION_FAILED,
        `Failed to connect to device ${this.device.name}`,
        error
      );
      PrinterEventManager.emit(PrinterEvent.PRINT_FAILED, { error: printerError });
      throw printerError;
    }
  }

  async disconnect(): Promise<void> {
    try {
      await PosPrinter.disconnectPrinter();
      this.connected = false;
      PrinterEventManager.emit(PrinterEvent.DEVICE_DISCONNECTED, { device: this.device });
    } catch (error) {
      const printerError = new PrinterError(
        PrinterErrorCode.DISCONNECTION_FAILED,
        `Failed to disconnect from device ${this.device.name}`,
        error
      );
      PrinterEventManager.emit(PrinterEvent.PRINT_FAILED, { error: printerError });
      throw printerError;
    }
  }

  async getStatus(): Promise<PrinterStatus> {
    try {
      return await PosPrinter.getStatus();
    } catch (error) {
      throw new PrinterError(
        PrinterErrorCode.STATUS_CHECK_FAILED,
        'Failed to get printer status',
        error
      );
    }
  }

  async sendRawCommand(command: number[]): Promise<void> {
    if (!this.connected) {
      throw new PrinterError(
        PrinterErrorCode.NOT_CONNECTED,
        'Device not connected'
      );
    }

    try {
      await PosPrinter.printRaw(command);
    } catch (error) {
      throw new PrinterError(
        PrinterErrorCode.PRINT_FAILED,
        'Failed to send raw command',
        error
      );
    }
  }

  async printText(text: string, options?: TextOptions): Promise<void> {
    if (!this.connected) {
      throw new PrinterError(
        PrinterErrorCode.NOT_CONNECTED,
        'Device not connected'
      );
    }

    if (!text || text.trim().length === 0) {
      throw new PrinterError(
        PrinterErrorCode.INVALID_TEXT,
        'Text cannot be empty'
      );
    }

    try {
      await PosPrinter.printText(text, options);
      PrinterEventManager.emit(PrinterEvent.PRINT_COMPLETED, { device: this.device, data: text });
    } catch (error) {
      const printerError = new PrinterError(
        PrinterErrorCode.PRINT_FAILED,
        'Failed to print text',
        error
      );
      PrinterEventManager.emit(PrinterEvent.PRINT_FAILED, { error: printerError });
      throw printerError;
    }
  }

  async printImage(imageUri: string, options?: ImageOptions): Promise<void> {
    if (!this.connected) {
      throw new PrinterError(
        PrinterErrorCode.NOT_CONNECTED,
        'Device not connected'
      );
    }

    if (!imageUri) {
      throw new PrinterError(
        PrinterErrorCode.INVALID_PARAMETER,
        'Image URI cannot be empty'
      );
    }

    try {
      await PosPrinter.printImage(imageUri, options);
      PrinterEventManager.emit(PrinterEvent.PRINT_COMPLETED, { device: this.device, data: imageUri });
    } catch (error) {
      const printerError = new PrinterError(
        PrinterErrorCode.PRINT_FAILED,
        'Failed to print image',
        error
      );
      PrinterEventManager.emit(PrinterEvent.PRINT_FAILED, { error: printerError });
      throw printerError;
    }
  }

  async printBarcode(data: string, type: keyof typeof BarcodeTypes, options?: BarcodeOptions): Promise<void> {
    if (!this.connected) {
      throw new PrinterError(
        PrinterErrorCode.NOT_CONNECTED,
        'Device not connected'
      );
    }

    if (!data) {
      throw new PrinterError(
        PrinterErrorCode.INVALID_PARAMETER,
        'Barcode data cannot be empty'
      );
    }

    try {
      await PosPrinter.printBarcode(data, type, options);
      PrinterEventManager.emit(PrinterEvent.PRINT_COMPLETED, { device: this.device, data });
    } catch (error) {
      const printerError = new PrinterError(
        PrinterErrorCode.PRINT_FAILED,
        'Failed to print barcode',
        error
      );
      PrinterEventManager.emit(PrinterEvent.PRINT_FAILED, { error: printerError });
      throw printerError;
    }
  }

  async printQRCode(data: string, options?: QRCodeOptions): Promise<void> {
    if (!this.connected) {
      throw new PrinterError(
        PrinterErrorCode.NOT_CONNECTED,
        'Device not connected'
      );
    }

    if (!data) {
      throw new PrinterError(
        PrinterErrorCode.INVALID_PARAMETER,
        'QR code data cannot be empty'
      );
    }

    try {
      await PosPrinter.printQRCode(data, options);
      PrinterEventManager.emit(PrinterEvent.PRINT_COMPLETED, { device: this.device, data });
    } catch (error) {
      const printerError = new PrinterError(
        PrinterErrorCode.PRINT_FAILED,
        'Failed to print QR code',
        error
      );
      PrinterEventManager.emit(PrinterEvent.PRINT_FAILED, { error: printerError });
      throw printerError;
    }
  }

  async cutPaper(): Promise<void> {
    if (!this.connected) {
      throw new PrinterError(
        PrinterErrorCode.NOT_CONNECTED,
        'Device not connected'
      );
    }

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

  async openCashDrawer(): Promise<void> {
    if (!this.connected) {
      throw new PrinterError(
        PrinterErrorCode.NOT_CONNECTED,
        'Device not connected'
      );
    }

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

  isConnected(): boolean {
    return this.connected;
  }

  getDevice(): ThermalPrinterNativeDevice {
    return this.device;
  }

  getName(): string {
    return this.device.name;
  }

  getAddress(): string {
    return this.device.address;
  }

  getType(): string {
    return this.device.type;
  }
}