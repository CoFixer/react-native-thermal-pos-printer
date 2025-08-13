package com.reactnativeposprinter;

public class PosPrinterConfig {
    private final int connectionTimeout;
    private final int readTimeout;
    private final boolean enableEvents;
    
    public PosPrinterConfig(int connectionTimeout, int readTimeout, boolean enableEvents) {
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
        this.enableEvents = enableEvents;
    }
    
    public int getConnectionTimeout() {
        return connectionTimeout;
    }
    
    public int getReadTimeout() {
        return readTimeout;
    }
    
    public boolean isEventsEnabled() {
        return enableEvents;
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
        
        public PosPrinterConfig build() {
            return new PosPrinterConfig(connectionTimeout, readTimeout, enableEvents);
        }
    }
}