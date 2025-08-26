package com.reactnativeposprinter

import android.graphics.*
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.OutputStream

class TextToBitmapHandler(private val context: android.content.Context) {

    companion object {
        private const val TAG = "TextToBitmap"
        private const val PRINTER_WIDTH_DOTS = 384
        private const val DEFAULT_PADDING = 4
        const val DEFAULT_LINE_SPACING = 1.0f
        private const val THRESHOLD_BASE = 128
        private const val PRINTER_DPI = 203
        private const val BITMAP_FONT_SCALE = 0.85f
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
        style: TextStyle = TextStyle(),
        scale: Float = BITMAP_FONT_SCALE,
        appendLineFeed: Boolean = false
    ): Boolean {
        return try {
            val bitmap = createTextBitmap(text, fontSizePt, letterSpacing, lineSpacing, style, scale)
            val monoBitmap = convertToMonochrome(bitmap)
            val escPosData = convertBitmapToEscPos(monoBitmap)
            val total = escPosData.size
            var offset = 0
            val CHUNK_SIZE = 470
            while (offset < total) {
                val chunkLen = kotlin.math.min(CHUNK_SIZE, total - offset)
                outputStream.write(escPosData, offset, chunkLen)
                outputStream.flush()
                try {
                    Thread.sleep(20)
                } catch (_: InterruptedException) {}
                offset += chunkLen
            }
            if (appendLineFeed) {
                try {
                    outputStream.flush()
                } catch (_: Exception) {}
            }
            try {
                Thread.sleep(20)
            } catch (_: InterruptedException) {}
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
        style: TextStyle = TextStyle(),
        scale: Float = BITMAP_FONT_SCALE
    ): String {
        return try {
            val bitmap = createTextBitmap(text, fontSizePt, letterSpacing, DEFAULT_LINE_SPACING, style, scale)
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
        style: TextStyle,
        scale: Float = BITMAP_FONT_SCALE
    ): Bitmap {
        val fontSizePx = convertPtToPx(fontSizePt, scale)
        val paint = createPaint(fontSizePx, letterSpacing, style)
        val textWidth = PRINTER_WIDTH_DOTS - (2 * DEFAULT_PADDING)
        val textLayout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, textWidth)
                .setAlignment(getLayoutAlignment(style.align))
                .setLineSpacing(0f, lineSpacing)
                .setIncludePad(true)
                .build()
        } else {
            StaticLayout(text, paint, textWidth, getLayoutAlignment(style.align), lineSpacing, 0f, false)
        }
        val bitmapHeight = textLayout.height + (2 * DEFAULT_PADDING)
        val bitmap = Bitmap.createBitmap(PRINTER_WIDTH_DOTS, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.save()
        canvas.translate(DEFAULT_PADDING.toFloat(), DEFAULT_PADDING.toFloat())
        textLayout.draw(canvas)
        canvas.restore()
        return bitmap
    }

    private fun getLayoutAlignment(align: String): android.text.Layout.Alignment {
        return when (align.lowercase()) {
            "center" -> android.text.Layout.Alignment.ALIGN_CENTER
            "right" -> android.text.Layout.Alignment.ALIGN_OPPOSITE
            else -> android.text.Layout.Alignment.ALIGN_NORMAL
        }
    }

    private fun createPaint(fontSizePx: Float, letterSpacing: Float, style: TextStyle): TextPaint {
        return TextPaint().apply {
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

    private fun convertPtToPx(pt: Float, scale: Float = 1.0f): Float {
        return pt * (PRINTER_DPI / 72f) * scale
    }

    private fun convertBitmapToEscPos(bitmap: Bitmap): ByteArray {
        val monoBitmap = convertToMonochrome(bitmap)
        return convertBitmapToGSv(monoBitmap)
    }

    private fun convertBitmapToGSv(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val width = bitmap.width.coerceAtMost(PRINTER_WIDTH_DOTS)
        val height = bitmap.height
        val CHUNK_HEIGHT = 24
        outputStream.write(byteArrayOf(0x1B, 0x40))
        
        for (startY in 0 until height step CHUNK_HEIGHT) {
            val endY = minOf(startY + CHUNK_HEIGHT, height)
            val chunkHeight = endY - startY
            outputStream.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00))

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
            try {
                Thread.sleep(30)
            } catch (_: InterruptedException) {}
        }
        return outputStream.toByteArray()
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
            pixels[i] = if (gray < THRESHOLD_BASE) Color.BLACK else Color.WHITE
        }
        monoBitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return monoBitmap
    }
}