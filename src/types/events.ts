import { ThermalPrinterNativeDevice, PrinterStatus } from './printer';
import { PrinterError } from './errors';

export enum PrinterEvent {
  DEVICE_CONNECTED = 'DEVICE_CONNECTED',
  DEVICE_DISCONNECTED = 'DEVICE_DISCONNECTED',
  DEVICE_FOUND = 'DEVICE_FOUND',
  PRINT_COMPLETED = 'PRINT_COMPLETED',
  PRINT_FAILED = 'PRINT_FAILED',
  STATUS_CHANGED = 'STATUS_CHANGED'
}

export interface PrinterEventData {
  device?: ThermalPrinterNativeDevice;
  error?: PrinterError;
  status?: PrinterStatus;
  data?: any;
}