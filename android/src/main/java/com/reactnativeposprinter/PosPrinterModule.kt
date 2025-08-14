@file:Suppress("DEPRECATION")
package com.reactnativeposprinter

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.core.DeviceEventManagerModule
import kotlinx.coroutines.*
import java.io.IOException
import java.io.OutputStream
import java.util.*

@ReactModule(name = PosPrinterModule.NAME)
class PosPrinterModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    
    companion object {
        const val NAME = "PosPrinter"
        private const val TAG = "PosPrinterModule"
        private val PRINTER_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        
        private val ESC_COMMANDS = mapOf(
            "INIT" to byteArrayOf(0x1B, 0x40),
            "CUT" to byteArrayOf(0x1D, 0x56, 0x00),
            "ALIGN_LEFT" to byteArrayOf(0x1B, 0x61, 0x00),
            "ALIGN_CENTER" to byteArrayOf(0x1B, 0x61, 0x01),
            "ALIGN_RIGHT" to byteArrayOf(0x1B, 0x61, 0x02),
            "BOLD_ON" to byteArrayOf(0x1B, 0x45, 0x01),
            "BOLD_OFF" to byteArrayOf(0x1B, 0x45, 0x00),
            "UNDERLINE_ON" to byteArrayOf(0x1B, 0x2D, 0x01),
            "UNDERLINE_OFF" to byteArrayOf(0x1B, 0x2D, 0x00),
            "SIZE_NORMAL" to byteArrayOf(0x1D, 0x21, 0x00),
            "SIZE_LARGE" to byteArrayOf(0x1D, 0x21, 0x11),
            "CASH_DRAWER" to byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte())
        )
        
        object Events {
            const val DEVICE_CONNECTED = "DEVICE_CONNECTED"
            const val DEVICE_DISCONNECTED = "DEVICE_DISCONNECTED"
            const val DEVICE_CONNECTION_LOST = "DEVICE_CONNECTION_LOST"
            const val PRINT_STATUS = "PRINT_STATUS"
        }
        
        object Errors {
            const val BLUETOOTH_NOT_AVAILABLE = "BLUETOOTH_NOT_AVAILABLE"
            const val BLUETOOTH_DISABLED = "BLUETOOTH_DISABLED"
            const val NOT_CONNECTED = "NOT_CONNECTED"
            const val CONNECTION_FAILED = "CONNECTION_FAILED"
            const val UNSUPPORTED_TYPE = "UNSUPPORTED_TYPE"
            const val PRINT_FAILED = "PRINT_FAILED"
        }
    }
    
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var connectionJob: Job? = null
    
    @Volatile
    private var isConnected = false
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun getName(): String {
        return NAME
    }
    
    override fun onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy()
        disconnectPrinter(null)
        scope.cancel()
    }
    
    @ReactMethod
    fun init(@Suppress("UNUSED_PARAMETER") options: ReadableMap?, promise: Promise) {
        try {
            if (bluetoothAdapter == null) {
                promise.reject(Errors.BLUETOOTH_NOT_AVAILABLE, "Bluetooth not available")
                return
            }
            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("INIT_ERROR", e.message, e)
        }
    }
    
    @ReactMethod
    fun getDeviceList(promise: Promise) {
        try {
            if (bluetoothAdapter == null) {
                promise.reject(Errors.BLUETOOTH_NOT_AVAILABLE, "Bluetooth not available")
                return
            }
            
            if (!bluetoothAdapter.isEnabled) {
                promise.reject(Errors.BLUETOOTH_DISABLED, "Bluetooth is disabled")
                return
            }
            
            val pairedDevices = bluetoothAdapter.bondedDevices
            val deviceList = Arguments.createArray()
            
            for (device in pairedDevices) {
                val deviceInfo = Arguments.createMap()
                deviceInfo.putString("name", device.name ?: "Unknown")
                deviceInfo.putString("address", device.address)
                deviceInfo.putString("type", "bluetooth")
                deviceList.pushMap(deviceInfo)
            }
            
            promise.resolve(deviceList)
        } catch (e: Exception) {
            promise.reject("GET_DEVICES_ERROR", e.message, e)
        }
    }
    
    @ReactMethod
    fun connectPrinter(address: String, type: String, promise: Promise) {
        when (type.lowercase()) {
            "bluetooth" -> connectBluetoothPrinter(address, promise)
            else -> promise.reject(Errors.UNSUPPORTED_TYPE, "Unsupported printer type: $type")
        }
    }
    
    @ReactMethod
    fun disconnectPrinter(promise: Promise?) {
        try {
            connectionJob?.cancel()
            outputStream?.close()
            bluetoothSocket?.close()
            isConnected = false
            
            sendEvent(Events.DEVICE_DISCONNECTED, Arguments.createMap())
            promise?.resolve(true)
        } catch (e: Exception) {
            promise?.reject("DISCONNECT_ERROR", e.message, e)
        }
    }
    
    @ReactMethod
    fun isConnected(promise: Promise) {
        promise.resolve(isConnected)
    }
    
    @ReactMethod
    fun printText(text: String, options: ReadableMap?, promise: Promise) {
        if (text.isEmpty()) {
            promise.reject(Errors.UNSUPPORTED_TYPE, "Text cannot be empty")
            return
        }
        
        executeWithConnection(promise) { stream ->
            try {
                options?.let { applyTextFormatting(it, stream) }
                stream.write(text.toByteArray(Charsets.UTF_8))
                stream.write("\n".toByteArray())
                stream.flush()
            } catch (e: Exception) {
                throw IOException("Failed to write text: ${e.message}", e)
            }
        }
    }
    
    @ReactMethod
    fun printImage(base64Image: String, options: ReadableMap?, promise: Promise) {
        executeWithConnection(promise) { stream ->
            try {
                val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                val rasterData = convertBitmapToRaster(bitmap)
                stream.write(rasterData)
            } catch (e: Exception) {
                throw IOException("Failed to process image: ${e.message}")
            }
        }
    }
    
    @ReactMethod
    fun printBarcode(data: String, type: String, options: ReadableMap?, promise: Promise) {
        executeWithConnection(promise) { stream ->
            // Basic barcode implementation
            stream.write(ESC_COMMANDS["ALIGN_CENTER"]!!)
            stream.write(data.toByteArray())
            stream.write("\n".toByteArray())
        }
    }
    
    @ReactMethod
    fun printQRCode(data: String, options: ReadableMap?, promise: Promise) {
        executeWithConnection(promise) { stream ->
            stream.write(ESC_COMMANDS["ALIGN_CENTER"]!!)
            stream.write(data.toByteArray())
            stream.write("\n".toByteArray())
        }
    }
    
    @ReactMethod
    fun cutPaper(promise: Promise) {
        executeWithConnection(promise) { stream ->
            stream.write(ESC_COMMANDS["CUT"]!!)
        }
    }
    
    @ReactMethod
    fun openCashDrawer(promise: Promise) {
        executeWithConnection(promise) { stream ->
            stream.write(ESC_COMMANDS["CASH_DRAWER"]!!)
        }
    }
    
    @ReactMethod
    fun sendRawCommand(command: ReadableArray, promise: Promise) {
        executeWithConnection(promise) { stream ->
            val bytes = ByteArray(command.size())
            for (i in 0 until command.size()) {
                bytes[i] = command.getInt(i).toByte()
            }
            stream.write(bytes)
        }
    }
    
    @ReactMethod
    fun getStatus(promise: Promise) {
        val status = Arguments.createMap()
        status.putBoolean("connected", isConnected)
        status.putString("platform", "android")
        promise.resolve(status)
    }
    
    private fun connectBluetoothPrinter(address: String, promise: Promise) {
        connectionJob = scope.launch {
            try {
                val device = bluetoothAdapter?.getRemoteDevice(address)
                if (device == null) {
                    withContext(Dispatchers.Main) {
                        promise.reject(Errors.CONNECTION_FAILED, "Device not found")
                    }
                    return@launch
                }
                
                bluetoothSocket = device.createRfcommSocketToServiceRecord(PRINTER_UUID)
                bluetoothSocket?.connect()
                outputStream = bluetoothSocket?.outputStream
                
                withContext(Dispatchers.Main) {
                    if (bluetoothSocket?.isConnected == true) {
                        isConnected = true
                        sendEvent(Events.DEVICE_CONNECTED, Arguments.createMap())
                        promise.resolve(true)
                    } else {
                        promise.reject(Errors.CONNECTION_FAILED, "Failed to connect")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    promise.reject(Errors.CONNECTION_FAILED, e.message, e)
                }
            }
        }
    }
    
    private inline fun executeWithConnection(promise: Promise, action: (OutputStream) -> Unit) {
        try {
            if (!isConnected || outputStream == null) {
                promise.reject(Errors.NOT_CONNECTED, "Printer not connected")
                return
            }
            
            // Check if socket is still connected
            if (bluetoothSocket?.isConnected != true) {
                isConnected = false
                outputStream = null
                sendEvent(Events.DEVICE_CONNECTION_LOST, Arguments.createMap())
                promise.reject(Errors.CONNECTION_FAILED, "Connection lost")
                return
            }
            
            action(outputStream!!)
            promise.resolve(true)
        } catch (e: IOException) {
            Log.e(TAG, "IO Error during print operation", e)
            // Handle connection lost
            isConnected = false
            outputStream = null
            sendEvent(Events.DEVICE_CONNECTION_LOST, Arguments.createMap())
            promise.reject(Errors.CONNECTION_FAILED, "Connection lost: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Print operation failed", e)
            promise.reject(Errors.CONNECTION_FAILED, "Print failed: ${e.message}", e)
        }
    }
    
    private fun applyTextFormatting(options: ReadableMap, stream: OutputStream) {
        if (options.hasKey("align")) {
            when (options.getString("align")?.uppercase()) {
                "LEFT" -> stream.write(ESC_COMMANDS["ALIGN_LEFT"]!!)
                "CENTER" -> stream.write(ESC_COMMANDS["ALIGN_CENTER"]!!)
                "RIGHT" -> stream.write(ESC_COMMANDS["ALIGN_RIGHT"]!!)
            }
        }
        
        if (options.hasKey("bold") && options.getBoolean("bold")) {
            stream.write(ESC_COMMANDS["BOLD_ON"]!!)
        }
        
        if (options.hasKey("underline") && options.getBoolean("underline")) {
            stream.write(ESC_COMMANDS["UNDERLINE_ON"]!!)
        }
        
        if (options.hasKey("size")) {
            val sizeValue = when (options.getType("size")) {
                ReadableType.Number -> {
                    val pixelSize = options.getInt("size")
                    FontSizeConstants.getPixelBasedSize(pixelSize)
                }
                ReadableType.String -> {
                    when (options.getString("size")?.uppercase()) {
                        "TINY" -> FontSizeConstants.getPixelBasedSize(8)
                        "SMALL" -> FontSizeConstants.getPixelBasedSize(10)
                        "NORMAL" -> FontSizeConstants.getPixelBasedSize(12)
                        "MEDIUM" -> FontSizeConstants.getPixelBasedSize(16)
                        "LARGE" -> FontSizeConstants.getPixelBasedSize(24)
                        "XLARGE" -> FontSizeConstants.getPixelBasedSize(36)
                        "XXLARGE" -> FontSizeConstants.getPixelBasedSize(48)
                        else -> 0x00
                    }
                }
                else -> 0x00
            }
            stream.write(byteArrayOf(0x1D, 0x21, sizeValue.toByte()))
        }
        
        if (options.hasKey("fontType")) {
            when (options.getString("fontType")?.uppercase()) {
                "A" -> stream.write(byteArrayOf(0x1B, 0x4D, 0x00))
                "B" -> stream.write(byteArrayOf(0x1B, 0x4D, 0x01))
                "C" -> stream.write(byteArrayOf(0x1B, 0x4D, 0x02))
            }
        }
        
        if (options.hasKey("italic") && options.getBoolean("italic")) {
            stream.write(byteArrayOf(0x1B, 0x34, 0x01))
        }
        
        if (options.hasKey("strikethrough") && options.getBoolean("strikethrough")) {
            stream.write(byteArrayOf(0x1B, 0x2D, 0x02))
        }
        
        if (options.hasKey("doubleStrike") && options.getBoolean("doubleStrike")) {
            stream.write(byteArrayOf(0x1B, 0x47, 0x01))
        }
        
        if (options.hasKey("invert") && options.getBoolean("invert")) {
            stream.write(byteArrayOf(0x1D, 0x42, 0x01))
        }
        
        if (options.hasKey("rotate")) {
            val rotation = options.getInt("rotate")
            when (rotation) {
                90 -> stream.write(byteArrayOf(0x1B, 0x56, 0x01))
                180 -> stream.write(byteArrayOf(0x1B, 0x7B, 0x01))
                270 -> stream.write(byteArrayOf(0x1B, 0x56, 0x01, 0x1B, 0x7B, 0x01))
                else -> stream.write(byteArrayOf(0x1B, 0x56, 0x00))
            }
        }
    }
    
    private fun convertBitmapToRaster(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val rasterData = mutableListOf<Byte>()
        
        rasterData.addAll(listOf(0x1D, 0x76, 0x30, 0x00).map { it.toByte() })
        
        val widthBytes = (width + 7) / 8
        rasterData.addAll(listOf(widthBytes and 0xFF, (widthBytes shr 8) and 0xFF).map { it.toByte() })
        rasterData.addAll(listOf(height and 0xFF, (height shr 8) and 0xFF).map { it.toByte() })
        
        for (y in 0 until height) {
            for (x in 0 until widthBytes) {
                var byte = 0
                for (bit in 0 until 8) {
                    val pixelX = x * 8 + bit
                    if (pixelX < width) {
                        val pixel = bitmap.getPixel(pixelX, y)
                        val gray = (0.299 * ((pixel shr 16) and 0xFF) + 
                                   0.587 * ((pixel shr 8) and 0xFF) + 
                                   0.114 * (pixel and 0xFF)).toInt()
                        if (gray < 128) {
                            byte = byte or (1 shl (7 - bit))
                        }
                    }
                }
                rasterData.add(byte.toByte())
            }
        }
        
        return rasterData.toByteArray()
    }
    
    private fun sendEvent(eventName: String, params: WritableMap) {
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }
}