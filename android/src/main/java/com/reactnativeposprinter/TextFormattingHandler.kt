package com.reactnativeposprinter

import android.content.Context
import android.util.Log
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType
import java.io.OutputStream

class TextFormattingHandler(private val printerService: Any?, private val context: Context) {
    private val enhancedFontHandler = EnhancedFontSizeHandler(context)
    
    data class FormattingResult(
        val shouldUseBitmapRendering: Boolean,
        val fontSizePt: Int? = null,
        val letterSpacing: Float = 1.0f
    )
    
    fun applyFormatting(options: ReadableMap, stream: OutputStream): FormattingResult {
        applyAlignment(options, stream)
        applyTextStyles(options, stream)
        
        val fontResult = applyFontSize(options, stream)
        
        applyFontType(options, stream)
        applyTextEffects(options, stream)
        applyRotation(options, stream)
        
        val letterSpacing = options.takeIf { it.hasKey("letterSpacing") }
            ?.getDouble("letterSpacing")?.toFloat() ?: 1.0f
        
        return FormattingResult(
            shouldUseBitmapRendering = fontResult.first,
            fontSizePt = fontResult.second,
            letterSpacing = letterSpacing
        )
    }
    
    private fun applyAlignment(options: ReadableMap, stream: OutputStream) {
        options.takeIf { it.hasKey("align") }
            ?.getString("align")?.uppercase()
            ?.let { alignment ->
                ESC_COMMANDS["ALIGN_$alignment"]?.let { command ->
                    stream.write(command)
                }
            }
    }
    
    private fun applyTextStyles(options: ReadableMap, stream: OutputStream) {
        if (options.hasKey("bold") && options.getBoolean("bold")) {
            stream.write(ESC_COMMANDS["BOLD_ON"]!!)
        }
        
        if (options.hasKey("underline") && options.getBoolean("underline")) {
            stream.write(ESC_COMMANDS["UNDERLINE_ON"]!!)
        }
        
        if (options.hasKey("italic") && options.getBoolean("italic")) {
            stream.write(ESC_COMMANDS["ITALIC_ON"]!!)
        }
    }
    
    private fun applyFontSize(options: ReadableMap, stream: OutputStream): Pair<Boolean, Int?> {
        if (!options.hasKey("size")) {
            return Pair(false, null)
        }
        
        val fontSizePt = parseFontSize(options)
        val success = enhancedFontHandler.applyEnhancedFontSize(fontSizePt, stream, printerService)
        
        return if (success) {
            Log.d(TAG, "Enhanced font sizing successful: ${enhancedFontHandler.getSizeDescription(fontSizePt)}")
            Pair(true, fontSizePt)
        } else {
            Log.w(TAG, "Enhanced font sizing failed for ${fontSizePt}pt")
            Pair(false, null)
        }
    }
    
    private fun parseFontSize(options: ReadableMap): Int {
        return when (options.getType("size")) {
            ReadableType.Number -> options.getInt("size")
            ReadableType.String -> {
                FONT_SIZE_MAP[options.getString("size")?.uppercase()] ?: DEFAULT_FONT_SIZE
            }
            else -> DEFAULT_FONT_SIZE
        }
    }
    
    private fun applyFontType(options: ReadableMap, stream: OutputStream) {
        options.takeIf { it.hasKey("fontType") }
            ?.getString("fontType")?.uppercase()
            ?.let { fontType ->
                FONT_TYPE_COMMANDS[fontType]?.let { command ->
                    stream.write(command)
                }
            }
    }
    
    private fun applyTextEffects(options: ReadableMap, stream: OutputStream) {
        TEXT_EFFECTS.forEach { (key, command) ->
            if (options.hasKey(key) && options.getBoolean(key)) {
                stream.write(command)
            }
        }
    }
    
    private fun applyRotation(options: ReadableMap, stream: OutputStream) {
        if (!options.hasKey("rotate")) return
        
        val rotation = options.getInt("rotate")
        val command = ROTATION_COMMANDS[rotation] ?: ROTATION_COMMANDS[0]!!
        stream.write(command)
    }
    
    companion object {
        private const val TAG = "TextFormattingHandler"
        private const val DEFAULT_FONT_SIZE = 9
        
        private val ESC_COMMANDS = mapOf(
            "ALIGN_LEFT" to byteArrayOf(0x1B, 0x61, 0x00),
            "ALIGN_CENTER" to byteArrayOf(0x1B, 0x61, 0x01),
            "ALIGN_RIGHT" to byteArrayOf(0x1B, 0x61, 0x02),
            "BOLD_ON" to byteArrayOf(0x1B, 0x45, 0x01),
            "UNDERLINE_ON" to byteArrayOf(0x1B, 0x2D, 0x01),
            "ITALIC_ON" to byteArrayOf(0x1B, 0x34, 0x01)
        )
        
        private val FONT_SIZE_MAP = mapOf(
            "TINY" to 6,
            "SMALL" to 8,
            "NORMAL" to 9,
            "MEDIUM" to 12,
            "LARGE" to 18,
            "XLARGE" to 27,
            "XXLARGE" to 36
        )
        
        private val FONT_TYPE_COMMANDS = mapOf(
            "A" to byteArrayOf(0x1B, 0x4D, 0x00),
            "B" to byteArrayOf(0x1B, 0x4D, 0x01),
            "C" to byteArrayOf(0x1B, 0x4D, 0x02)
        )
        
        private val TEXT_EFFECTS = mapOf(
            "strikethrough" to byteArrayOf(0x1B, 0x2D, 0x02),
            "doubleStrike" to byteArrayOf(0x1B, 0x47, 0x01),
            "invert" to byteArrayOf(0x1D, 0x42, 0x01)
        )
        
        private val ROTATION_COMMANDS = mapOf(
            0 to byteArrayOf(0x1B, 0x56, 0x00),
            90 to byteArrayOf(0x1B, 0x56, 0x01),
            180 to byteArrayOf(0x1B, 0x7B, 0x01),
            270 to byteArrayOf(0x1B, 0x56, 0x01, 0x1B, 0x7B, 0x01)
        )
    }
}