import { NativeModules } from 'react-native';
import type { Spec } from '../types/spec';

const { PosPrinter } = NativeModules;

if (!PosPrinter) {
  throw new Error(
    'PosPrinter native module is not available. Make sure the library is properly linked.'
  );
}

const PosPrinterModule: Spec = {
  init: (options) => PosPrinter.init(options),
  getDeviceList: () => PosPrinter.getDeviceList(),
  connectPrinter: (address, type) => PosPrinter.connectPrinter(address, type),
  disconnectPrinter: () => PosPrinter.disconnectPrinter(),
  isConnected: () => PosPrinter.isConnected(),
  printText: (text, options) => PosPrinter.printText(text, options),
  printImage: (base64Image, options) => PosPrinter.printImage(base64Image, options),
  printBitmap: (base64Data) => PosPrinter.printBitmap(base64Data),
  printBarcode: (data, type, options) => PosPrinter.printBarcode(data, type, options),
  printQRCode: (data, options) => PosPrinter.printQRCode(data, options),
  cutPaper: () => PosPrinter.cutPaper(),
  openCashDrawer: () => PosPrinter.openCashDrawer(),
  sendRawCommand: (command) => PosPrinter.sendRawCommand(command),
  getStatus: () => PosPrinter.getStatus(),
};

export { PosPrinterModule as PosPrinter };
export default PosPrinterModule;