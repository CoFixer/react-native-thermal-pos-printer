package com.reactnativeposprinter

import android.content.Context
import android.util.Log
import java.io.OutputStream

class EnhancedFontSizeHandler(private val context: Context) {
    
    companion object {
        private const val TAG = "EnhancedFontSize"
    }
    
    private val textToBitmapHandler = TextToBitmapHandler(context)
    
    fun applyEnhancedFontSize(
        fontSizePt: Int, 
        @Suppress("UNUSED_PARAMETER") outputStream: OutputStream, 
        @Suppress("UNUSED_PARAMETER") printerService: Any? = null
    ): Boolean {
        Log.d(TAG, "Applying enhanced font size via bitmap: ${fontSizePt}pt")
        
        return try {
            if (fontSizePt <= 0) {
                Log.w(TAG, "Invalid font size: $fontSizePt")
                return false
            }
            
            PosPrinterModule.setLastFontSizeMethod("bitmap_rendering")
            Log.d(TAG, "Enhanced font size applied successfully: ${getSizeDescription(fontSizePt)}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set bitmap rendering mode: ${e.message}")
            false
        }
    }
    
    fun printTextWithFont(
        text: String,
        fontSizePt: Int,
        outputStream: OutputStream,
        letterSpacing: Float = 1.0f
    ): Boolean {
        if (text.isEmpty()) {
            Log.w(TAG, "Cannot print empty text")
            return false
        }
        
        if (fontSizePt <= 0) {
            Log.w(TAG, "Invalid font size for text printing: $fontSizePt")
            return false
        }
        
        return try {
            Log.d(TAG, "Printing text as bitmap - Size: ${getSizeDescription(fontSizePt)}, Letter spacing: $letterSpacing")
            textToBitmapHandler.printTextAsBitmap(text, fontSizePt.toFloat(), outputStream, letterSpacing)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to print text with font: ${e.message}")
            false
        }
    }
    
    fun getTextAsBase64(
        text: String,
        fontSizePt: Int,
        letterSpacing: Float = 1.0f
    ): String {
        if (text.isEmpty()) {
            Log.w(TAG, "Cannot convert empty text to bitmap")
            return ""
        }
        
        return try {
            textToBitmapHandler.getTextAsBitmap64(text, fontSizePt.toFloat(), letterSpacing)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert text to base64: ${e.message}")
            ""
        }
    }
    
    fun getSizeDescription(fontSizePt: Int): String {
        return when {
            fontSizePt <= 6 -> "Tiny (${fontSizePt}pt)"
            fontSizePt <= 9 -> "Small (${fontSizePt}pt)"
            fontSizePt <= 13 -> "Normal (${fontSizePt}pt)"
            fontSizePt <= 18 -> "Medium (${fontSizePt}pt)"
            fontSizePt <= 24 -> "Large (${fontSizePt}pt)"
            fontSizePt <= 30 -> "Extra Large (${fontSizePt}pt)"
            fontSizePt <= 36 -> "XXL (${fontSizePt}pt)"
            else -> "Maximum (${fontSizePt}pt)"
        }
    }
}
