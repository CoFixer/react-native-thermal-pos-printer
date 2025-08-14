package com.reactnativeposprinter

data class PosPrinterConfig(
    val connectionTimeout: Int = DEFAULT_CONNECTION_TIMEOUT,
    val readTimeout: Int = DEFAULT_READ_TIMEOUT,
    val enableEvents: Boolean = DEFAULT_ENABLE_EVENTS,
    val retryAttempts: Int = DEFAULT_RETRY_ATTEMPTS,
    val bufferSize: Int = DEFAULT_BUFFER_SIZE
) {
    
    companion object {
        const val DEFAULT_CONNECTION_TIMEOUT = 10_000
        const val DEFAULT_READ_TIMEOUT = 5_000
        const val DEFAULT_ENABLE_EVENTS = true
        const val DEFAULT_RETRY_ATTEMPTS = 3
        const val DEFAULT_BUFFER_SIZE = 1024
        
        private const val MIN_TIMEOUT = 1_000
        private const val MAX_TIMEOUT = 60_000
        private const val MIN_RETRY_ATTEMPTS = 0
        private const val MAX_RETRY_ATTEMPTS = 10
        private const val MIN_BUFFER_SIZE = 512
        private const val MAX_BUFFER_SIZE = 8192
        
        fun default(): PosPrinterConfig = PosPrinterConfig()
        
        fun fastConnection(): PosPrinterConfig = PosPrinterConfig(
            connectionTimeout = 5_000,
            readTimeout = 2_000,
            retryAttempts = 1
        )
        
        fun slowConnection(): PosPrinterConfig = PosPrinterConfig(
            connectionTimeout = 30_000,
            readTimeout = 15_000,
            retryAttempts = 5
        )
    }
    
    init {
        validateConfiguration()
    }
    
    private fun validateConfiguration() {
        require(connectionTimeout in MIN_TIMEOUT..MAX_TIMEOUT) {
            "Connection timeout must be between $MIN_TIMEOUT and $MAX_TIMEOUT ms, got: $connectionTimeout"
        }
        require(readTimeout in MIN_TIMEOUT..MAX_TIMEOUT) {
            "Read timeout must be between $MIN_TIMEOUT and $MAX_TIMEOUT ms, got: $readTimeout"
        }
        require(retryAttempts in MIN_RETRY_ATTEMPTS..MAX_RETRY_ATTEMPTS) {
            "Retry attempts must be between $MIN_RETRY_ATTEMPTS and $MAX_RETRY_ATTEMPTS, got: $retryAttempts"
        }
        require(bufferSize in MIN_BUFFER_SIZE..MAX_BUFFER_SIZE) {
            "Buffer size must be between $MIN_BUFFER_SIZE and $MAX_BUFFER_SIZE bytes, got: $bufferSize"
        }
    }
    
    class Builder {
        private var connectionTimeout = DEFAULT_CONNECTION_TIMEOUT
        private var readTimeout = DEFAULT_READ_TIMEOUT
        private var enableEvents = DEFAULT_ENABLE_EVENTS
        private var retryAttempts = DEFAULT_RETRY_ATTEMPTS
        private var bufferSize = DEFAULT_BUFFER_SIZE
        
        fun setConnectionTimeout(timeout: Int): Builder = apply {
            this.connectionTimeout = timeout
        }
        
        fun setReadTimeout(timeout: Int): Builder = apply {
            this.readTimeout = timeout
        }
        
        fun setEnableEvents(enable: Boolean): Builder = apply {
            this.enableEvents = enable
        }
        
        fun setRetryAttempts(attempts: Int): Builder = apply {
            this.retryAttempts = attempts
        }
        
        fun setBufferSize(size: Int): Builder = apply {
            this.bufferSize = size
        }
        
        fun build(): PosPrinterConfig = PosPrinterConfig(
            connectionTimeout = connectionTimeout,
            readTimeout = readTimeout,
            enableEvents = enableEvents,
            retryAttempts = retryAttempts,
            bufferSize = bufferSize
        )
    }
}