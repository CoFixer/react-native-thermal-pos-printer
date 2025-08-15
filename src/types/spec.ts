import { PrinterStatus } from './printer';

export interface Spec {
  init(options?: Record<string, any>): Promise<void>;
  getDeviceList(): Promise<any[]>;
  connectPrinter(address: string, type: string): Promise<boolean>;
  disconnectPrinter(): Promise<boolean>;
  isConnected(): Promise<boolean>;
  printText(text: string, options?: Record<string, any>): Promise<boolean>;
  printImage(base64Image: string, options?: Record<string, any>): Promise<boolean>;
  printBitmap(base64Data: string): Promise<boolean>;
  printBarcode(data: string, type: string, options?: Record<string, any>): Promise<boolean>;
  printQRCode(data: string, options?: Record<string, any>): Promise<boolean>;
  cutPaper(): Promise<boolean>;
  openCashDrawer(): Promise<boolean>;
  sendRawCommand(command: number[]): Promise<boolean>;
  getStatus(): Promise<PrinterStatus>;
}