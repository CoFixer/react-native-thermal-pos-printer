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

export interface ThermalPrinterNativeDevice {
  name: string;
  address: string;
  id: string;
  type: 'BLUETOOTH' | 'USB' | 'WIFI' | 'ETHERNET';
  connected: boolean;
  rssi?: number;
  batteryLevel?: number;
  bondState?: number;
  deviceClass?: string;
  extra?: Map<string, Object>;
}

export interface ConnectionOptions {
  timeout?: number;
  encoding?: 'UTF-8' | 'GBK' | 'GB2312' | 'BIG5';
  delimiter?: string;
  secure?: boolean;
}