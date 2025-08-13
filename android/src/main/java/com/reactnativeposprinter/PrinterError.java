package com.reactnativeposprinter;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

public class PrinterError extends Exception {
    private final PrinterErrorCode errorCode;
    private final String nativeMessage;
    
    public enum PrinterErrorCode {
        BLUETOOTH_NOT_SUPPORTED("BLUETOOTH_NOT_SUPPORTED"),
        BLUETOOTH_NOT_ENABLED("BLUETOOTH_NOT_ENABLED"),
        DEVICE_NOT_FOUND("DEVICE_NOT_FOUND"),
        CONNECTION_FAILED("CONNECTION_FAILED"),
        PERMISSION_DENIED("PERMISSION_DENIED"),
        PRINT_FAILED("PRINT_FAILED"),
        INVALID_DATA("INVALID_DATA"),
        NOT_CONNECTED("NOT_CONNECTED");
        
        private final String code;
        
        PrinterErrorCode(String code) {
            this.code = code;
        }
        
        public String getCode() {
            return code;
        }
    }
    
    public PrinterError(PrinterErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.nativeMessage = message;
    }
    
    public PrinterError(PrinterErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.nativeMessage = message;
    }
    
    public WritableMap toWritableMap() {
        WritableMap error = Arguments.createMap();
        error.putString("code", errorCode.getCode());
        error.putString("message", getMessage());
        error.putString("nativeMessage", nativeMessage);
        return error;
    }
}