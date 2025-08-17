@file:Suppress("DEPRECATION")
package com.reactnativeposprinter

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.util.Base64
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.core.DeviceEventManagerModule
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*

@ReactModule(name = PosPrinterModule.NAME)
class PosPrinterModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    private var printerService: Any? = null

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
            "CASH_DRAWER" to byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte())
        )

        private var lastFontSizeMethod: String = "unknown"
    
        fun getLastFontSizeMethod(): String = lastFontSizeMethod
        fun setLastFontSizeMethod(method: String) {
            lastFontSizeMethod = method
        }
        
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
    override fun getName(): String = NAME
    override fun onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy()
        scope.cancel()
        disconnectPrinter(null)
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
                val deviceInfo = Arguments.createMap().apply {
                    putString("name", device.name ?: "Unknown")
                    putString("address", device.address)
                    putString("type", "bluetooth")
                }
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
                var usedBitmapRendering = false
                options?.let { 
                    val formattingResult = applyTextFormatting(it, stream)
                    
                    val alignValue = options.getString("align")?.lowercase() ?: "left"
                    
                    val needsBitmapForFont = options.hasKey("fontType") && options.getString("fontType") != "A"
                    val needsBitmapForStyling = (options.hasKey("bold") && options.getBoolean("bold")) ||
                                              (options.hasKey("italic") && options.getBoolean("italic")) ||
                                              (options.hasKey("underline") && options.getBoolean("underline"))
                    val needsBitmapForSize = options.hasKey("size") && (options.getInt("size") > 0)
                    val needsBitmapForAlignment = alignValue != "left"
                    
                    if (formattingResult.shouldUseBitmapRendering || needsBitmapForFont || needsBitmapForStyling || needsBitmapForSize || needsBitmapForAlignment) {
                        val textToBitmapHandler = TextToBitmapHandler(reactApplicationContext)
                        
                        val fontFamily = if (options.hasKey("fontType")) {
                            when (options.getString("fontType")?.uppercase()) {
                                "A" -> "monospace"
                                "B" -> "sans-serif" 
                                "C" -> "serif"
                                else -> "monospace"
                            }
                        } else "monospace"
                        
                        val isBold = if (options.hasKey("bold")) options.getBoolean("bold") else false
                        val isItalic = if (options.hasKey("italic")) options.getBoolean("italic") else false
                        val isUnderline = if (options.hasKey("underline")) options.getBoolean("underline") else false
                        val isStrikethrough = if (options.hasKey("strikethrough")) options.getBoolean("strikethrough") else false
                        val doubleWidth = if (options.hasKey("doubleWidth")) options.getBoolean("doubleWidth") else false
                        val doubleHeight = if (options.hasKey("doubleHeight")) options.getBoolean("doubleHeight") else false
                        
                        val style = TextToBitmapHandler.TextStyle(
                            isBold = isBold,
                            isItalic = isItalic,
                            isUnderline = isUnderline,
                            isStrikethrough = isStrikethrough,
                            fontFamily = fontFamily,
                            align = alignValue,
                            doubleWidth = doubleWidth,
                            doubleHeight = doubleHeight
                        )
                        
                        val fontSize = formattingResult.fontSizePt?.toFloat() ?: 12.0f
                        
                        usedBitmapRendering = textToBitmapHandler.printTextAsBitmap(
                            text, 
                            fontSize, 
                            stream, 
                            formattingResult.letterSpacing,
                            style = style
                        )
                    } else {
                        stream.write(ESC_COMMANDS["ALIGN_LEFT"]!!)
                    }
                } ?: run {
                    stream.write(ESC_COMMANDS["ALIGN_LEFT"]!!)
                }
                
                if (!usedBitmapRendering) {
                    stream.write(text.toByteArray(Charsets.UTF_8))
                }
                
                stream.flush()
            } catch (e: Exception) {
                throw IOException("Failed to write text: ${e.message}", e)
            }
        }
    }
    
    private fun applyTextFormatting(options: ReadableMap, stream: OutputStream): TextFormattingHandler.FormattingResult {
        val formattingHandler = TextFormattingHandler(printerService, reactApplicationContext)
        return formattingHandler.applyFormatting(options, stream)
    }

    @ReactMethod
    fun printImage(base64Image: String, options: ReadableMap?, promise: Promise) {
        executeWithConnection(promise) { stream ->
            try {
                val align = options?.getString("align") ?: "LEFT"
                when (align.uppercase()) {
                    "CENTER" -> stream.write(ESC_COMMANDS["ALIGN_CENTER"]!!)
                    "RIGHT" -> stream.write(ESC_COMMANDS["ALIGN_RIGHT"]!!)
                    else -> stream.write(ESC_COMMANDS["ALIGN_LEFT"]!!)
                }

                val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                val factoryOptions = BitmapFactory.Options().apply { inSampleSize = 2 }
                var originalBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, factoryOptions)
                originalBitmap = convertToWhiteBackground(originalBitmap)

                val maxWidth = 384
                val chunkHeight = 8
                val widthAligned = (maxWidth / 8) * 8
                val aspectRatio = originalBitmap.height.toFloat() / originalBitmap.width
                val scaledHeight = (widthAligned * aspectRatio).toInt()
                val paddedHeight = ((scaledHeight + chunkHeight - 1) / chunkHeight) * chunkHeight

                val finalBitmap = Bitmap.createBitmap(widthAligned, paddedHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(finalBitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawColor(Color.WHITE)
                canvas.drawBitmap(originalBitmap, Rect(0, 0, originalBitmap.width, originalBitmap.height), Rect(0, 0, widthAligned, scaledHeight), paint)

                val rasterBytes = convertBitmapToRasterChunks(finalBitmap, chunkHeight)
                for (chunk in rasterBytes) {
                    stream.write(chunk)
                    Thread.sleep(50)
                }

                stream.write(byteArrayOf(0x1B, 0x33, 0x00))
                stream.write(byteArrayOf(0x0A, 0x0A))
                stream.write(ESC_COMMANDS["ALIGN_LEFT"]!!)

            } catch (e: Exception) {
                throw IOException("Failed to print image: ${e.message}")
            }
        }
    }

    private fun convertToWhiteBackground(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    private fun convertBitmapToRasterChunks(bitmap: Bitmap, chunkHeight: Int): List<ByteArray> {
        val width = bitmap.width
        val height = bitmap.height
        val widthBytes = width / 8
        val chunks = mutableListOf<ByteArray>()
        var y = 0

        while (y < height) {
            val blockHeight = minOf(chunkHeight, height - y)
            val baos = ByteArrayOutputStream()

            baos.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00))
            baos.write(widthBytes and 0xFF)
            baos.write((widthBytes shr 8) and 0xFF)
            baos.write(blockHeight and 0xFF)
            baos.write((blockHeight shr 8) and 0xFF)

            for (row in 0 until blockHeight) {
                for (byteX in 0 until widthBytes) {
                    var byte = 0
                    for (bit in 0 until 8) {
                        val x = byteX * 8 + bit
                        val pixel = bitmap.getPixel(x, y + row)
                        val r = (pixel shr 16) and 0xFF
                        val g = (pixel shr 8) and 0xFF
                        val b = pixel and 0xFF
                        val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                        if (luminance < 127) byte = byte or (1 shl (7 - bit))
                    }
                    baos.write(byte)
                }
            }

            chunks.add(baos.toByteArray())
            y += blockHeight
        }

        return chunks
    }

    @ReactMethod
    fun printBarcode(data: String, type: String, options: ReadableMap?, promise: Promise) {
        executeWithConnection(promise) { stream ->
            try {
                val align = options?.getString("align") ?: "CENTER"
                val height = options?.getInt("height") ?: 60
                val width = options?.getInt("width") ?: 2
                val textPosition = options?.getString("textPosition") ?: "BELOW"
                
                when (align.uppercase()) {
                    "LEFT" -> stream.write(ESC_COMMANDS["ALIGN_LEFT"]!!)
                    "CENTER" -> stream.write(ESC_COMMANDS["ALIGN_CENTER"]!!)
                    "RIGHT" -> stream.write(ESC_COMMANDS["ALIGN_RIGHT"]!!)
                }
                
                val barcodeType = when (type.uppercase()) {
                    "UPC_A" -> 0
                    "UPC_E" -> 1
                    "EAN13" -> 2
                    "EAN8" -> 3
                    "CODE39" -> 4
                    "ITF" -> 5
                    "CODABAR" -> 6
                    "CODE93" -> 7
                    "CODE128" -> 8
                    else -> 8
                }
                
                stream.write(byteArrayOf(0x1D, 0x68, height.toByte()))
                stream.write(byteArrayOf(0x1D, 0x77, width.toByte()))
                
                val hriPosition = when (textPosition.uppercase()) {
                    "NONE" -> 0
                    "ABOVE" -> 1
                    "BELOW" -> 2
                    "BOTH" -> 3
                    else -> 2
                }
                stream.write(byteArrayOf(0x1D, 0x48, hriPosition.toByte()))
                
                stream.write(byteArrayOf(0x1D, 0x6B, barcodeType.toByte()))
                stream.write(data.toByteArray())
                stream.write(byteArrayOf(0x00))
                
                stream.write(ESC_COMMANDS["ALIGN_LEFT"]!!)
            } catch (e: Exception) {
                throw IOException("Failed to print barcode: ${e.message}", e)
            }
        }
    }
    
    @ReactMethod
    fun printQRCode(data: String, options: ReadableMap?, promise: Promise) {
        executeWithConnection(promise) { stream ->
            try {
                val align = options?.getString("align") ?: "CENTER"
                val size = options?.getInt("size") ?: 5
                val errorLevel = options?.getString("errorLevel") ?: "M"
                
                when (align.uppercase()) {
                    "LEFT" -> stream.write(ESC_COMMANDS["ALIGN_LEFT"]!!)
                    "CENTER" -> stream.write(ESC_COMMANDS["ALIGN_CENTER"]!!)
                    "RIGHT" -> stream.write(ESC_COMMANDS["ALIGN_RIGHT"]!!)
                }
                
                stream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00))
                stream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, size.toByte()))
                
                val errorLevelByte = when (errorLevel.uppercase()) {
                    "L" -> 0x30.toByte()
                    "M" -> 0x31.toByte()
                    "Q" -> 0x32.toByte()
                    "H" -> 0x33.toByte()
                    else -> 0x31.toByte()
                }
                stream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, errorLevelByte))
                
                val dataBytes = data.toByteArray(Charsets.UTF_8)
                val dataLength = dataBytes.size + 3
                val pL = (dataLength and 0xFF).toByte()
                val pH = ((dataLength shr 8) and 0xFF).toByte()
                
                stream.write(byteArrayOf(0x1D, 0x28, 0x6B, pL, pH, 0x31, 0x50, 0x30))
                stream.write(dataBytes)
                
                stream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30))
                
                stream.flush()
                Thread.sleep(100)
                
                stream.write(ESC_COMMANDS["ALIGN_LEFT"]!!)
            } catch (e: Exception) {
                throw IOException("Failed to print QR code: ${e.message}", e)
            }
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
        val status = Arguments.createMap().apply {
            putBoolean("connected", isConnected)
            putString("platform", "android")
        }
        promise.resolve(status)
    }

    @ReactMethod
    fun enterPrinterBuffer(clear: Boolean, promise: Promise) {
        try {
            printerService?.let { service ->
                val method = service.javaClass.getMethod("enterPrinterBuffer", Boolean::class.java)
                method.invoke(service, clear)
                promise.resolve(true)
            } ?: promise.reject("NO_SERVICE", "Printer service not available")
        } catch (e: Exception) {
            promise.reject("BUFFER_ERROR", "enterPrinterBuffer failed: ${e.message}")
        }
    }
    
    @ReactMethod
    fun exitPrinterBuffer(commit: Boolean, promise: Promise) {
        try {
            printerService?.let { service ->
                val method = service.javaClass.getMethod("exitPrinterBuffer", Boolean::class.java)
                method.invoke(service, commit)
                promise.resolve(true)
            } ?: promise.reject("NO_SERVICE", "Printer service not available")
        } catch (e: Exception) {
            promise.reject("BUFFER_ERROR", "exitPrinterBuffer failed: ${e.message}")
        }
    }
    
    @ReactMethod
    fun commitPrinterBuffer(promise: Promise) {
        try {
            printerService?.let { service ->
                val method = service.javaClass.getMethod("commitPrinterBuffer")
                method.invoke(service)
                promise.resolve(true)
            } ?: promise.reject("NO_SERVICE", "Printer service not available")
        } catch (e: Exception) {
            promise.reject("BUFFER_ERROR", "commitPrinterBuffer failed: ${e.message}")
        }
    }

    @ReactMethod
    fun resetPrinter(promise: Promise) {
        executeWithConnection(promise) { stream ->
            stream.write(ESC_COMMANDS["INIT"]!!)
            stream.flush()
        }
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

                        outputStream?.write(ESC_COMMANDS["INIT"]!!)
                        outputStream?.flush()

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
        if (!isConnected || outputStream == null) {
            promise.reject(Errors.NOT_CONNECTED, "Printer not connected")
            return
        }
        
        try {
            action(outputStream!!)
            promise.resolve(null)
        } catch (e: IOException) {
            Log.e(TAG, "Print operation failed", e)
            promise.reject(Errors.CONNECTION_FAILED, "Print failed: ${e.message}", e)
        }
    }
    
    private fun sendEvent(eventName: String, params: WritableMap) {
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    private fun convertToMonochrome(bitmap: Bitmap): Bitmap {
        val monoBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(monoBitmap)

        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        }

        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        val pixels = IntArray(bitmap.width * bitmap.height)
        monoBitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (i in pixels.indices) {
            val gray = Color.red(pixels[i])
            pixels[i] = if (gray < 128) Color.BLACK else Color.WHITE
        }

        monoBitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return monoBitmap
    }
}