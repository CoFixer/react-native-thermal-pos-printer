package com.reactnativeposprinter;

import androidx.annotation.NonNull;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PosPrinterPackage implements ReactPackage {
    
    private final PosPrinterConfig config;
    
    public PosPrinterPackage() {
        this.config = new PosPrinterConfig.Builder().build();
    }
    
    public PosPrinterPackage(PosPrinterConfig config) {
        this.config = config;
    }
    
    @NonNull
    @Override
    public List<NativeModule> createNativeModules(@NonNull ReactApplicationContext reactContext) {
        List<NativeModule> modules = new ArrayList<>();
        modules.add(new PosPrinterModule(reactContext, config));
        return modules;
    }
    
    @NonNull
    @Override
    public List<ViewManager> createViewManagers(@NonNull ReactApplicationContext reactContext) {
        return Collections.emptyList();
    }
    
    public static class Builder {
        private int connectionTimeout = 10000;
        private int readTimeout = 5000;
        private boolean enableEvents = true;
        
        public Builder setConnectionTimeout(int timeout) {
            this.connectionTimeout = timeout;
            return this;
        }
        
        public Builder setReadTimeout(int timeout) {
            this.readTimeout = timeout;
            return this;
        }
        
        public Builder setEnableEvents(boolean enable) {
            this.enableEvents = enable;
            return this;
        }
        
        public PosPrinterPackage build() {
            return new PosPrinterPackage(new PosPrinterConfig(connectionTimeout, readTimeout, enableEvents));
        }
    }
}