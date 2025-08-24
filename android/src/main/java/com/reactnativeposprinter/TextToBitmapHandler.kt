package com.reactnativeposprinter

import android.graphics.*
import android.util.Log
import android.util.TypedValue
import java.io.OutputStream
import java.io.ByteArrayOutputStream
import android.util.Base64
import android.content.Context

class TextToBitmapHandler(private val context: Context) {
    
    companion object {
        private const val TAG = "TextToBitmap"
        private const val PRINTER_WIDTH_DOTS = 384
        private const val DEFAULT_PADDING = 4
        private const val DEFAULT_LINE_SPACING = 1.2f
        private const val THRESHOLD_BASE = 128
        private const val THRESHOLD_VARIANCE = 32
    }
    
    data class TextStyle(
        val isBold: Boolean = false,
        val isItalic: Boolean = false,
        val isUnderline: Boolean = false,
        val isStrikethrough: Boolean = false,
        val fontFamily: String = "monospace",
        val align: String = "left", 
        val doubleWidth: Boolean = false,
        val doubleHeight: Boolean = false
    )
    
    fun printTextAsBitmap(
        text: String,
        fontSizePt: Float,
        outputStream: OutputStream,
        letterSpacing: Float = 1.0f,
        lineSpacing: Float = DEFAULT_LINE_SPACING,
        style: TextStyle = TextStyle()
    ): Boolean {
        return try {
            val bitmap = createTextBitmap(text, fontSizePt, letterSpacing, lineSpacing, style)
            val monoBitmap = convertToMonochrome(bitmap)
            val escPosData = convertBitmapToEscPos(monoBitmap)
            outputStream.write(escPosData)
            outputStream.flush()
            try {
                Thread.sleep(30)
            } catch (_: InterruptedException) {
                // ignore
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error printing text as bitmap", e)
            false
        }
    }
    
    fun getTextAsBitmap64(
        text: String,
        fontSizePt: Float,
        letterSpacing: Float = 1.0f,
        style: TextStyle = TextStyle()
    ): String {
        return try {
            val bitmap = createTextBitmap(text, fontSizePt, letterSpacing, DEFAULT_LINE_SPACING, style)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert text to base64: ${e.message}")
            ""
        }
    }
    
    private fun createTextBitmap(
        text: String,
        fontSizePt: Float,
        letterSpacing: Float,
        lineSpacing: Float,
        style: TextStyle
    ): Bitmap {
        val fontSizePx = convertPtToPx(fontSizePt)
        val paint = createPaint(fontSizePx, letterSpacing, style)
        val actualTextWidth = paint.measureText(text)
        var textHeight = (paint.textSize * lineSpacing).toInt()
        
        if (style.doubleWidth) {
            paint.textScaleX = 2.0f
        }
        if (style.doubleHeight) {
            textHeight *= 2
            paint.textSize = fontSizePx * 2
        }
        
        val bitmapWidth = PRINTER_WIDTH_DOTS
        val bitmapHeight = textHeight + DEFAULT_PADDING * 2
        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        canvas.drawColor(Color.WHITE)
        val availableWidth = bitmapWidth - (DEFAULT_PADDING * 2)
        val finalTextWidth = if (style.doubleWidth) actualTextWidth * 2 else actualTextWidth
        
        val x = when (style.align.lowercase()) {
            "center" -> {
                val centerX = DEFAULT_PADDING + (availableWidth - finalTextWidth) / 2f
                centerX
            }
            "right" -> {
                val rightX = DEFAULT_PADDING + availableWidth - finalTextWidth
                rightX
            }
            else -> {
                DEFAULT_PADDING.toFloat()
            }
        }
        
        val fontMetrics = paint.fontMetrics
        val y = DEFAULT_PADDING - fontMetrics.top
        canvas.drawText(text, x, y, paint)
        
        return bitmap
    }
    
    private fun createPaint(fontSizePx: Float, letterSpacing: Float, style: TextStyle): Paint {
        return Paint().apply {
            textSize = fontSizePx
            color = Color.BLACK
            isAntiAlias = true
            typeface = createTypeface(style)
            this.letterSpacing = letterSpacing - 1.0f
            isUnderlineText = style.isUnderline
            isStrikeThruText = style.isStrikethrough
            
            if (style.isBold) {
                strokeWidth = 1.5f
                this.style = Paint.Style.FILL_AND_STROKE
            }
        }
    }
    
    private fun createTypeface(style: TextStyle): Typeface {
        val baseTypeface = when (style.fontFamily.lowercase()) {
            "serif" -> Typeface.SERIF
            "sans-serif" -> Typeface.SANS_SERIF
            "monospace" -> Typeface.MONOSPACE
            else -> Typeface.MONOSPACE
        }
        
        val typefaceStyle = when {
            style.isBold && style.isItalic -> Typeface.BOLD_ITALIC
            style.isBold -> Typeface.BOLD
            style.isItalic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        
        return Typeface.create(baseTypeface, typefaceStyle)
    }
    
    private fun convertPtToPx(pt: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_PT,
            pt,
            context.resources.displayMetrics
        )
    }
    
    private fun convertBitmapToEscPos(bitmap: Bitmap): ByteArray {
        val monoBitmap = convertToMonochrome(bitmap)
        return convertBitmapToGSv(monoBitmap)
    }
    
    private fun convertBitmapToGSv(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val width = bitmap.width.coerceAtMost(384)
        val height = bitmap.height
        val CHUNK_HEIGHT = 24 
        
        for (startY in 0 until height step CHUNK_HEIGHT) {
            val endY = minOf(startY + CHUNK_HEIGHT, height)
            val chunkHeight = endY - startY
            
            outputStream.write(byteArrayOf(0x1B, 0x40))
            outputStream.write(0x1D) 
            outputStream.write(0x76)
            outputStream.write(0x30)
            outputStream.write(0x00)  
            
            val widthBytes = (width + 7) / 8
            outputStream.write(widthBytes and 0xFF)
            outputStream.write((widthBytes shr 8) and 0xFF)
            outputStream.write(chunkHeight and 0xFF)
            outputStream.write((chunkHeight shr 8) and 0xFF)
            
            for (y in startY until endY) {
                for (x in 0 until widthBytes) {
                    var byte = 0
                    for (bit in 0 until 8) {
                        val pixelX = x * 8 + bit
                        if (pixelX < width) {
                            val pixel = bitmap.getPixel(pixelX, y)
                            if (Color.red(pixel) < THRESHOLD_BASE) {
                                byte = byte or (1 shl (7 - bit))
                            }
                        }
                    }
                    outputStream.write(byte)
                }
            }

            Thread.sleep(20)
        }
        
        return outputStream.toByteArray()
    }

    private fun convertToMonochrome(bitmap: Bitmap): Bitmap {
        val monoBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(monoBitmap)
        
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                setSaturation(0f)
            })
        }
        
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        val pixels = IntArray(bitmap.width * bitmap.height)
        monoBitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        for (i in pixels.indices) {
            val gray = Color.red(pixels[i])
            pixels[i] = if (gray < THRESHOLD_BASE) Color.BLACK else Color.WHITE
        }
        
        monoBitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return monoBitmap
    }

    private fun createMultiLineTextBitmap(
        text: String,
        fontSizePt: Float,
        letterSpacing: Float,
        lineSpacing: Float,
        style: TextStyle
    ): Bitmap {
        val fontSizePx = convertPtToPx(fontSizePt)
        val paint = createPaint(fontSizePx, letterSpacing, style)
        
        val lines = text.split("\n")
        val lineHeight = (paint.textSize * lineSpacing).toInt()
        val fontMetrics = paint.fontMetrics
        
        var maxWidth = 0
        val lineWidths = mutableListOf<Int>()
        
        lines.forEach { line ->
            val textBounds = Rect()
            paint.getTextBounds(line, 0, line.length, textBounds)
            val width = textBounds.width()
            lineWidths.add(width)
            maxWidth = maxOf(maxWidth, width)
        }
        
        val bitmapWidth = minOf((maxWidth + DEFAULT_PADDING * 2), PRINTER_WIDTH_DOTS)
        val bitmapHeight = (lines.size * lineHeight) + DEFAULT_PADDING * 2
        
        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        
        lines.forEachIndexed { index, line ->
            val lineWidth = lineWidths[index]
            val x = when (style.align.lowercase()) {
                "center" -> (bitmapWidth - lineWidth) / 2f
                "right" -> bitmapWidth - lineWidth - DEFAULT_PADDING.toFloat()
                else -> DEFAULT_PADDING.toFloat()
            }
            
            val y = DEFAULT_PADDING.toFloat() + (index + 1) * lineHeight - fontMetrics.descent
            canvas.drawText(line, x, y, paint)
        }
        
        return bitmap
    }
}
