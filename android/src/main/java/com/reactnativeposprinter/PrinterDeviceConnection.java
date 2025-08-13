package com.reactnativeposprinter;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class PrinterDeviceConnection {
    private static final UUID PRINTER_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    
    private final BluetoothDevice device;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private boolean connected = false;
    private final PosPrinterModule module;
    
    public PrinterDeviceConnection(BluetoothDevice device, PosPrinterModule module) {
        this.device = device;
        this.module = module;
    }
    
    public void connect() throws PrinterError {
        try {
            if (socket != null && socket.isConnected()) {
                socket.close();
            }
            
            socket = device.createRfcommSocketToServiceRecord(PRINTER_UUID);
            socket.connect();
            outputStream = socket.getOutputStream();
            connected = true;
            
            // Initialize printer
            outputStream.write(new byte[]{0x1B, 0x40}); // ESC_INIT
            outputStream.flush();
            
        } catch (IOException e) {
            connected = false;
            throw new PrinterError(PrinterError.PrinterErrorCode.CONNECTION_FAILED, 
                "Failed to connect to printer: " + e.getMessage(), e);
        }
    }
    
    public void disconnect() throws PrinterError {
        try {
            if (outputStream != null) {
                outputStream.close();
            }
            if (socket != null && socket.isConnected()) {
                socket.close();
            }
            connected = false;
        } catch (IOException e) {
            throw new PrinterError(PrinterError.PrinterErrorCode.CONNECTION_FAILED, 
                "Failed to disconnect: " + e.getMessage(), e);
        }
    }
    
    public boolean isConnected() {
        return connected && socket != null && socket.isConnected();
    }
    
    public void write(byte[] data) throws PrinterError {
        if (!isConnected()) {
            throw new PrinterError(PrinterError.PrinterErrorCode.NOT_CONNECTED, 
                "Printer is not connected");
        }
        
        try {
            outputStream.write(data);
            outputStream.flush();
        } catch (IOException e) {
            connected = false;
            throw new PrinterError(PrinterError.PrinterErrorCode.PRINT_FAILED, 
                "Failed to write data: " + e.getMessage(), e);
        }
    }
    
    public WritableMap getDeviceInfo() {
        WritableMap deviceInfo = Arguments.createMap();
        deviceInfo.putString("name", device.getName());
        deviceInfo.putString("address", device.getAddress());
        deviceInfo.putString("type", "BLUETOOTH");
        deviceInfo.putBoolean("connected", isConnected());
        deviceInfo.putInt("bondState", device.getBondState());
        return deviceInfo;
    }
}