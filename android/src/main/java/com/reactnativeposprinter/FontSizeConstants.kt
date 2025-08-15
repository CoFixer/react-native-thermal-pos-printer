package com.reactnativeposprinter

import android.content.Intent
import android.util.Log
import java.io.OutputStream

object FontSizeConstants {
    private val PIXEL_SIZE_MAP = mapOf(
        8 to 0x00,
        10 to 0x01,
        12 to 0x10,
        16 to 0x11,
        20 to 0x22,
        24 to 0x33,
        28 to 0x44,
        32 to 0x55,
        36 to 0x66,
        48 to 0x77
    )
    
    fun tryHardwareFontSize(pixelSize: Int, outputStream: OutputStream, printerService: Any?): Boolean {
        return try {
            val success = attemptBufferedHardwareAPI(pixelSize, printerService)
            if (success) {
                PosPrinterModule.setLastFontSizeMethod("hardware_api")
                Log.d("FontSizeConstants", "Hardware API font sizing successful: ${pixelSize}px")
                true
            } else {
                PosPrinterModule.setLastFontSizeMethod("esc_pos_fallback")
                false
            }
        } catch (e: Exception) {
            PosPrinterModule.setLastFontSizeMethod("esc_pos_fallback")
            Log.w("FontSizeConstants", "Hardware API not available, falling back to ESC/POS", e)
            false
        }
    }
    
    private fun attemptBufferedHardwareAPI(pixelSize: Int, printerService: Any?): Boolean {
        printerService?.let { service ->
            return try {
                val enterMethod = service.javaClass.getMethod("enterPrinterBuffer", Boolean::class.java)
                enterMethod.invoke(service, true)
                
                val fontSizeSuccess = tryDirectFontSizeAPI(pixelSize, service)
                
                if (fontSizeSuccess) {
                    val commitMethod = service.javaClass.getMethod("commitPrinterBuffer")
                    commitMethod.invoke(service)
                    true
                } else {
                    val exitMethod = service.javaClass.getMethod("exitPrinterBuffer", Boolean::class.java)
                    exitMethod.invoke(service, false)
                    false
                }
            } catch (e: Exception) {
                try {
                    val exitMethod = service.javaClass.getMethod("exitPrinterBuffer", Boolean::class.java)
                    exitMethod.invoke(service, false)
                } catch (exitException: Exception) {
                    Log.w("FontSizeConstants", "Failed to exit buffer after error", exitException)
                }
                false
            }
        }
        return false
    }
    
    private fun tryDirectFontSizeAPI(pixelSize: Int, service: Any): Boolean {
        return try {
            val setFontSizeMethod = service.javaClass.getMethod("setFontSize", Float::class.java)
            setFontSizeMethod.invoke(service, pixelSize.toFloat())
            true
        } catch (e: Exception) {
            tryAlternativeFontSizeAPI(pixelSize, service)
        }
    }
    
    private fun tryAlternativeFontSizeAPI(pixelSize: Int, service: Any): Boolean {
        return try {
            val setTextSizeMethod = service.javaClass.getMethod("setTextSize", Int::class.java)
            setTextSizeMethod.invoke(service, pixelSize)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun getPixelBasedSize(pixelSize: Int): Int {
        return PIXEL_SIZE_MAP[pixelSize] ?: calculateDynamicSize(pixelSize)
    }
    
    private fun calculateDynamicSize(pixelSize: Int): Int {
        val widthMultiplier = when {
            pixelSize <= 8 -> 0
            pixelSize <= 12 -> 1
            pixelSize <= 16 -> 2
            pixelSize <= 24 -> 3
            pixelSize <= 32 -> 4
            pixelSize <= 40 -> 5
            pixelSize <= 48 -> 6
            else -> 7
        }
        
        val heightMultiplier = widthMultiplier
        return (widthMultiplier shl 4) or heightMultiplier
    }
    
    fun getActualPixelSize(multiplier: Int): Pair<Int, Int> {
        val widthMultiplier = (multiplier shr 4) and 0x0F
        val heightMultiplier = multiplier and 0x0F
        
        val baseWidth = 8
        val baseHeight = 16
        
        return Pair(
            baseWidth * (widthMultiplier + 1),
            baseHeight * (heightMultiplier + 1)
        )
    }
}