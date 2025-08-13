package com.reactnativeposprinter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

// Add these imports
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.ReactContext;

@ReactModule(name = PosPrinterModule.NAME)
public class PosPrinterModule extends ReactContextBaseJavaModule {
    public static final String NAME = "PosPrinter";
    private static final String TAG = "PosPrinterModule";
    private static final UUID PRINTER_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private boolean isConnected = false;
    
    private static final byte[] ESC_INIT = {0x1B, 0x40};
    private static final byte[] ESC_CUT = {0x1D, 0x56, 0x00};
    private static final byte[] ESC_ALIGN_LEFT = {0x1B, 0x61, 0x00};
    private static final byte[] ESC_ALIGN_CENTER = {0x1B, 0x61, 0x01};
    private static final byte[] ESC_ALIGN_RIGHT = {0x1B, 0x61, 0x02};
    private static final byte[] ESC_BOLD_ON = {0x1B, 0x45, 0x01};
    private static final byte[] ESC_BOLD_OFF = {0x1B, 0x45, 0x00};
    private static final byte[] ESC_UNDERLINE_ON = {0x1B, 0x2D, 0x01};
    private static final byte[] ESC_UNDERLINE_OFF = {0x1B, 0x2D, 0x00};
    private static final byte[] ESC_SIZE_NORMAL = {0x1D, 0x21, 0x00};
    private static final byte[] ESC_SIZE_LARGE = {0x1D, 0x21, 0x11};
    private static final byte[] ESC_CASH_DRAWER = {0x1B, 0x70, 0x00, 0x19, (byte) 0xFA};

    public PosPrinterModule(ReactApplicationContext reactContext) {
        super(reactContext);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    @NonNull
    public String getName() {
        return NAME;
    }

    @ReactMethod
    public void init(Promise promise) {
        try {
            if (bluetoothAdapter == null) {
                promise.reject("BLUETOOTH_NOT_SUPPORTED", "Bluetooth is not supported on this device");
                return;
            }
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("INIT_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void getDeviceList(Promise promise) {
        try {
            WritableArray deviceList = Arguments.createArray();
            
            if (bluetoothAdapter == null) {
                promise.reject("BLUETOOTH_NOT_SUPPORTED", "Bluetooth is not supported on this device");
                return;
            }
            
            if (!bluetoothAdapter.isEnabled()) {
                promise.reject("BLUETOOTH_DISABLED", "Bluetooth is not enabled");
                return;
            }
            
            // Check for permissions (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (getCurrentActivity().checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    promise.reject("PERMISSION_DENIED", "BLUETOOTH_CONNECT permission is required");
                    return;
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (getCurrentActivity().checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    promise.reject("PERMISSION_DENIED", "Location permission is required for Bluetooth device discovery");
                    return;
                }
            }
            
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            
            for (BluetoothDevice device : pairedDevices) {
                WritableMap deviceMap = Arguments.createMap();
                deviceMap.putString("name", device.getName() != null ? device.getName() : "Unknown Device");
                deviceMap.putString("address", device.getAddress());
                deviceMap.putString("type", "BLUETOOTH");
                deviceMap.putBoolean("connected", false);
                deviceMap.putInt("bondState", device.getBondState());
                deviceList.pushMap(deviceMap);
            }
            
            promise.resolve(deviceList);
        } catch (Exception e) {
            promise.reject("GET_DEVICES_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void connectPrinter(String address, String type, Promise promise) {
        try {
            if ("BLUETOOTH".equals(type)) {
                connectBluetoothPrinter(address, promise);
            } else {
                promise.reject("UNSUPPORTED_TYPE", "Connection type not supported: " + type);
            }
        } catch (Exception e) {
            promise.reject("CONNECT_ERROR", e.getMessage());
        }
    }

    // Add event constants
    public static final String EVENT_DEVICE_CONNECTED = "DEVICE_CONNECTED";
    public static final String EVENT_DEVICE_DISCONNECTED = "DEVICE_DISCONNECTED";
    public static final String EVENT_DEVICE_CONNECTION_LOST = "DEVICE_CONNECTION_LOST";
    public static final String EVENT_PRINT_STATUS = "PRINT_STATUS";
    
    // Add event emitter method
    private void sendEvent(String eventName, WritableMap params) {
        getReactApplicationContext()
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
    }
    
    // Update connectBluetoothPrinter method
    private void connectBluetoothPrinter(String address, Promise promise) {
        try {
            if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                bluetoothSocket.close();
            }
            
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            bluetoothSocket = device.createRfcommSocketToServiceRecord(PRINTER_UUID);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            isConnected = true;
            
            outputStream.write(ESC_INIT);
            outputStream.flush();
            
            // Emit connection event
            WritableMap eventData = Arguments.createMap();
            eventData.putString("address", address);
            eventData.putString("name", device.getName());
            sendEvent(EVENT_DEVICE_CONNECTED, eventData);
            
            promise.resolve(true);
        } catch (IOException e) {
            isConnected = false;
            
            // Emit connection failed event
            WritableMap eventData = Arguments.createMap();
            eventData.putString("address", address);
            eventData.putString("error", e.getMessage());
            sendEvent(EVENT_DEVICE_CONNECTION_LOST, eventData);
            
            promise.reject(new PrinterError(PrinterError.PrinterErrorCode.CONNECTION_FAILED, e.getMessage(), e).toWritableMap());
        }
    }
}

    @ReactMethod
    public void disconnectPrinter(Promise promise) {
        try {
            if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                outputStream.close();
                bluetoothSocket.close();
            }
            isConnected = false;
            promise.resolve(true);
        } catch (IOException e) {
            promise.reject("DISCONNECT_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void isConnected(Promise promise) {
        promise.resolve(isConnected && bluetoothSocket != null && bluetoothSocket.isConnected());
    }

    @ReactMethod
    public void printText(String text, ReadableMap options, Promise promise) {
        try {
            if (!isConnected || outputStream == null) {
                promise.reject("NOT_CONNECTED", "Printer is not connected");
                return;
            }

            applyTextFormatting(options);
            
            outputStream.write(text.getBytes("UTF-8"));
            outputStream.write("\n".getBytes());
            outputStream.flush();
            
            resetFormatting();
            
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("PRINT_TEXT_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void printImage(String base64, ReadableMap options, Promise promise) {
        try {
            if (!isConnected || outputStream == null) {
                promise.reject("NOT_CONNECTED", "Printer is not connected");
                return;
            }
    
            byte[] imageBytes = Base64.decode(base64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            
            if (bitmap == null) {
                promise.reject("INVALID_IMAGE", "Invalid image data");
                return;
            }
    
            byte[] imageData = convertBitmapToEscPos(bitmap);
            outputStream.write(imageData);
            outputStream.flush();
            
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("PRINT_IMAGE_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void printQRCode(String data, ReadableMap options, Promise promise) {
        try {
            if (!isConnected || outputStream == null) {
                promise.reject("NOT_CONNECTED", "Printer is not connected");
                return;
            }

            byte[] qrCommands = generateQRCodeCommands(data, options);
            outputStream.write(qrCommands);
            outputStream.flush();
            
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("PRINT_QRCODE_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void cutPaper(Promise promise) {
        try {
            if (!isConnected || outputStream == null) {
                promise.reject("NOT_CONNECTED", "Printer is not connected");
                return;
            }

            outputStream.write(ESC_CUT);
            outputStream.flush();
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("CUT_PAPER_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void openCashDrawer(Promise promise) {
        try {
            if (!isConnected || outputStream == null) {
                promise.reject("NOT_CONNECTED", "Printer is not connected");
                return;
            }

            outputStream.write(ESC_CASH_DRAWER);
            outputStream.flush();
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("CASH_DRAWER_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void printRaw(ReadableArray data, Promise promise) {
        try {
            if (!isConnected || outputStream == null) {
                promise.reject("NOT_CONNECTED", "Printer is not connected");
                return;
            }

            byte[] rawData = new byte[data.size()];
            for (int i = 0; i < data.size(); i++) {
                rawData[i] = (byte) data.getInt(i);
            }
            
            outputStream.write(rawData);
            outputStream.flush();
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("PRINT_RAW_ERROR", e.getMessage());
        }
    }

    private void applyTextFormatting(ReadableMap options) throws IOException {
        if (options.hasKey("align")) {
            String align = options.getString("align");
            switch (align) {
                case "CENTER":
                    outputStream.write(ESC_ALIGN_CENTER);
                    break;
                case "RIGHT":
                    outputStream.write(ESC_ALIGN_RIGHT);
                    break;
                default:
                    outputStream.write(ESC_ALIGN_LEFT);
                    break;
            }
        }

        if (options.hasKey("bold") && options.getBoolean("bold")) {
            outputStream.write(ESC_BOLD_ON);
        }

        if (options.hasKey("underline") && options.getBoolean("underline")) {
            outputStream.write(ESC_UNDERLINE_ON);
        }

        if (options.hasKey("size")) {
            if (options.getType("size") == com.facebook.react.bridge.ReadableType.Number) {
                int fontSize = options.getInt("size");
                byte[] sizeCommand = generateFontSizeCommand(fontSize);
                outputStream.write(sizeCommand);
            } else {
                String size = options.getString("size");
                if ("LARGE".equals(size) || "XLARGE".equals(size)) {
                    outputStream.write(ESC_SIZE_LARGE);
                } else {
                    outputStream.write(ESC_SIZE_NORMAL);
                }
            }
        }
    }

    private byte[] generateFontSizeCommand(int fontSize) {
        int widthMultiplier = 0;
        int heightMultiplier = 0;
        
        if (fontSize <= 12) {
            widthMultiplier = 0;
            heightMultiplier = 0;
        } else if (fontSize <= 18) {
            widthMultiplier = 1;
            heightMultiplier = 1;
        } else if (fontSize <= 24) {
            widthMultiplier = 2;
            heightMultiplier = 2;
        } else if (fontSize <= 36) {
            widthMultiplier = 3;
            heightMultiplier = 3;
        } else if (fontSize <= 48) {
            widthMultiplier = 4;
            heightMultiplier = 4;
        } else {
            widthMultiplier = 5;
            heightMultiplier = 5;
        }
        
        widthMultiplier = Math.min(widthMultiplier, 7);
        heightMultiplier = Math.min(heightMultiplier, 7);
        
        byte sizeValue = (byte) ((widthMultiplier << 4) | heightMultiplier);
        
        return new byte[]{0x1D, 0x21, sizeValue};
    }

    @ReactMethod
    public void setFontSize(int fontSize, Promise promise) {
        try {
            if (!isConnected || outputStream == null) {
                promise.reject("NOT_CONNECTED", "Printer is not connected");
                return;
            }

            byte[] sizeCommand = generateFontSizeCommand(fontSize);
            outputStream.write(sizeCommand);
            outputStream.flush();
            
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("SET_FONT_SIZE_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void resetFontSettings(Promise promise) {
        try {
            if (!isConnected || outputStream == null) {
                promise.reject("NOT_CONNECTED", "Printer is not connected");
                return;
            }

            resetFormatting();
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("RESET_FONT_ERROR", e.getMessage());
        }
    }

    private void resetFormatting() throws IOException {
        outputStream.write(ESC_ALIGN_LEFT);
        outputStream.write(ESC_BOLD_OFF);
        outputStream.write(ESC_UNDERLINE_OFF);
        outputStream.write(ESC_SIZE_NORMAL);
    }

    private byte[] convertBitmapToEscPos(Bitmap bitmap) {
        int printerWidth = 384;
        
        float ratio = (float) printerWidth / bitmap.getWidth();
        int scaledHeight = (int) (bitmap.getHeight() * ratio);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, printerWidth, scaledHeight, true);
        
        Bitmap monoBitmap = convertToMonochrome(scaledBitmap);
        
        return convertToRasterFormat(monoBitmap);
    }

    private Bitmap convertToMonochrome(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap monoBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = bitmap.getPixel(x, y);
                int gray = (int) (0.299 * ((pixel >> 16) & 0xFF) + 
                             0.587 * ((pixel >> 8) & 0xFF) + 
                             0.114 * (pixel & 0xFF));
                int monoPixel = gray > 128 ? 0xFFFFFF : 0x000000;
                monoBitmap.setPixel(x, y, monoPixel);
            }
        }
        return monoBitmap;
    }

    private byte[] convertToRasterFormat(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        byte[] header = {0x1D, 0x76, 0x30, 0x00};
        
        int bytesPerLine = (width + 7) / 8;
        
        byte[] sizeParams = {
            (byte) (bytesPerLine & 0xFF),
            (byte) ((bytesPerLine >> 8) & 0xFF),
            (byte) (height & 0xFF),
            (byte) ((height >> 8) & 0xFF)
        };
        
        byte[] imageData = new byte[bytesPerLine * height];
        int index = 0;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < bytesPerLine; x++) {
                byte b = 0;
                for (int bit = 0; bit < 8; bit++) {
                    int pixelX = x * 8 + bit;
                    if (pixelX < width) {
                        int pixel = bitmap.getPixel(pixelX, y);
                        if ((pixel & 0xFF) < 128) {
                            b |= (1 << (7 - bit));
                        }
                    }
                }
                imageData[index++] = b;
            }
        }
        
        byte[] result = new byte[header.length + sizeParams.length + imageData.length];
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(sizeParams, 0, result, header.length, sizeParams.length);
        System.arraycopy(imageData, 0, result, header.length + sizeParams.length, imageData.length);
        
        return result;
    }

    private byte[] generateQRCodeCommands(String data, ReadableMap options) {
        try {
            int size = options.hasKey("size") ? options.getInt("size") : 6;
            String align = options.hasKey("align") ? options.getString("align") : "LEFT";
            String errorLevel = options.hasKey("errorLevel") ? options.getString("errorLevel") : "M";
            
            size = Math.max(1, Math.min(16, size));
            
            byte errorLevelByte;
            switch (errorLevel) {
                case "L": errorLevelByte = 48; break;
                case "M": errorLevelByte = 49; break;
                case "Q": errorLevelByte = 50; break;
                case "H": errorLevelByte = 51; break;
                default: errorLevelByte = 49; break;
            }
            
            byte[] dataBytes = data.getBytes("UTF-8");
            int dataLength = dataBytes.length;
            
            int totalLength = 0;
            
            byte[] alignCommand = getAlignmentCommand(align);
            totalLength += alignCommand.length;
            
            byte[] modelCmd = {0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00};
            totalLength += modelCmd.length;
            
            byte[] sizeCmd = {0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, (byte)size};
            totalLength += sizeCmd.length;
            
            byte[] errorCmd = {0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, errorLevelByte};
            totalLength += errorCmd.length;
            
            byte[] storeCmd = new byte[8 + dataLength];
            storeCmd[0] = 0x1D;
            storeCmd[1] = 0x28;
            storeCmd[2] = 0x6B;
            storeCmd[3] = (byte)((dataLength + 3) & 0xFF);
            storeCmd[4] = (byte)(((dataLength + 3) >> 8) & 0xFF);
            storeCmd[5] = 0x31;
            storeCmd[6] = 0x50;
            storeCmd[7] = 0x30;
            System.arraycopy(dataBytes, 0, storeCmd, 8, dataLength);
            totalLength += storeCmd.length;
            
            byte[] printCmd = {0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30};
            totalLength += printCmd.length;
            
            byte[] lineFeed = {0x0A};
            totalLength += lineFeed.length;
            
            byte[] result = new byte[totalLength];
            int offset = 0;
            
            System.arraycopy(alignCommand, 0, result, offset, alignCommand.length);
            offset += alignCommand.length;
            
            System.arraycopy(modelCmd, 0, result, offset, modelCmd.length);
            offset += modelCmd.length;
            
            System.arraycopy(sizeCmd, 0, result, offset, sizeCmd.length);
            offset += sizeCmd.length;
            
            System.arraycopy(errorCmd, 0, result, offset, errorCmd.length);
            offset += errorCmd.length;
            
            System.arraycopy(storeCmd, 0, result, offset, storeCmd.length);
            offset += storeCmd.length;
            
            System.arraycopy(printCmd, 0, result, offset, printCmd.length);
            offset += printCmd.length;
            
            System.arraycopy(lineFeed, 0, result, offset, lineFeed.length);
            
            return result;
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating QR code commands", e);
            return new byte[0];
        }
    }

    private byte[] generateBarcodeCommands(String data, String type, ReadableMap options) {
        try {
            int width = options.hasKey("width") ? options.getInt("width") : 2;
            int height = options.hasKey("height") ? options.getInt("height") : 162;
            String align = options.hasKey("align") ? options.getString("align") : "LEFT";
            String textPosition = options.hasKey("textPosition") ? options.getString("textPosition") : "BELOW";
            
            width = Math.max(2, Math.min(6, width));
            height = Math.max(1, Math.min(255, height));
            
            byte barcodeType = getBarcodeType(type);
            if (barcodeType == -1) {
                throw new IllegalArgumentException("Unsupported barcode type: " + type);
            }
            
            byte[] alignCommand = getAlignmentCommand(align);
            
            byte[] widthCmd = {0x1D, 0x77, (byte)width};
            
            byte[] heightCmd = {0x1D, 0x68, (byte)height};
            
            byte textPos = getTextPosition(textPosition);
            byte[] textPosCmd = {0x1D, 0x48, textPos};
            
            byte[] dataBytes = data.getBytes("UTF-8");
            byte[] barcodeCmd = new byte[4 + dataBytes.length];
            barcodeCmd[0] = 0x1D;
            barcodeCmd[1] = 0x6B;
            barcodeCmd[2] = barcodeType;
            barcodeCmd[3] = (byte)dataBytes.length;
            System.arraycopy(dataBytes, 0, barcodeCmd, 4, dataBytes.length);
            
            byte[] lineFeed = {0x0A};
            
            int totalLength = alignCommand.length + widthCmd.length + heightCmd.length + 
                             textPosCmd.length + barcodeCmd.length + lineFeed.length;
            byte[] result = new byte[totalLength];
            int offset = 0;
            
            System.arraycopy(alignCommand, 0, result, offset, alignCommand.length);
            offset += alignCommand.length;
            
            System.arraycopy(widthCmd, 0, result, offset, widthCmd.length);
            offset += widthCmd.length;
            
            System.arraycopy(heightCmd, 0, result, offset, heightCmd.length);
            offset += heightCmd.length;
            
            System.arraycopy(textPosCmd, 0, result, offset, textPosCmd.length);
            offset += textPosCmd.length;
            
            System.arraycopy(barcodeCmd, 0, result, offset, barcodeCmd.length);
            offset += barcodeCmd.length;
            
            System.arraycopy(lineFeed, 0, result, offset, lineFeed.length);
            
            return result;
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating barcode commands", e);
            return new byte[0];
        }
    }

    private byte[] getAlignmentCommand(String align) {
        switch (align) {
            case "CENTER": return ESC_ALIGN_CENTER;
            case "RIGHT": return ESC_ALIGN_RIGHT;
            default: return ESC_ALIGN_LEFT;
        }
    }

    private byte getBarcodeType(String type) {
        switch (type.toUpperCase()) {
            case "UPC_A": return 0;
            case "UPC_E": return 1;
            case "EAN13": return 2;
            case "EAN8": return 3;
            case "CODE39": return 4;
            case "ITF": return 5;
            case "CODABAR": return 6;
            case "CODE93": return 72;
            case "CODE128": return 73;
            default: return -1;
        }
    }

    private byte getTextPosition(String position) {
        switch (position) {
            case "NONE": return 0;
            case "ABOVE": return 1;
            case "BELOW": return 2;
            case "BOTH": return 3;
            default: return 2;
        }
    }
}