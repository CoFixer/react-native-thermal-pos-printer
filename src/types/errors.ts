export enum PrinterErrorCode {
  CONNECTION_FAILED = 'CONNECTION_FAILED',
  DEVICE_NOT_FOUND = 'DEVICE_NOT_FOUND',
  PRINT_FAILED = 'PRINT_FAILED',
  DEVICE_NOT_CONNECTED = 'DEVICE_NOT_CONNECTED',
  INVALID_PARAMETER = 'INVALID_PARAMETER',
  TIMEOUT = 'TIMEOUT',
  PERMISSION_DENIED = 'PERMISSION_DENIED',
  HARDWARE_ERROR = 'HARDWARE_ERROR',
  INIT_FAILED = 'INIT_FAILED',
  DEVICE_LIST_FAILED = 'DEVICE_LIST_FAILED',
  DISCONNECTION_FAILED = 'DISCONNECTION_FAILED',
  INVALID_TEXT = 'INVALID_TEXT',
  STATUS_CHECK_FAILED = 'STATUS_CHECK_FAILED',
  NOT_CONNECTED = 'NOT_CONNECTED',
  PRINT_SUCCESS = 'PRINT_SUCCESS',
  BLUETOOTH_NOT_SUPPORTED = 'BLUETOOTH_NOT_SUPPORTED',
  BLUETOOTH_NOT_ENABLED = 'BLUETOOTH_NOT_ENABLED',
  INVALID_IMAGE = 'INVALID_IMAGE',
}

export class PrinterError extends Error {
  code: PrinterErrorCode;
  nativeError?: any;

  constructor(code: PrinterErrorCode, message: string, nativeError?: any) {
    super(message);
    this.name = 'PrinterError';
    this.code = code;
    this.nativeError = nativeError;
  }

  static fromNativeError(nativeError: any): PrinterError {
    return new PrinterError(PrinterErrorCode.HARDWARE_ERROR, nativeError.message || 'Unknown error', nativeError);
  }
}