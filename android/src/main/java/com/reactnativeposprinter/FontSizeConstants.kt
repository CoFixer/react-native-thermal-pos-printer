package com.reactnativeposprinter

object FontSizeConstants {
    
    // ESC/POS base: Font A = 12x24 pixels (width x height)
    private val PIXEL_SIZE_MAP = mapOf(
        6 to 0x00, 7 to 0x00, 8 to 0x00, 9 to 0x00, 10 to 0x00,
        11 to 0x00, 12 to 0x00, 13 to 0x00, 14 to 0x00, 15 to 0x00,
        16 to 0x00, 17 to 0x00, 18 to 0x00,
        
        19 to 0x01, 20 to 0x01, 21 to 0x01, 22 to 0x01, 23 to 0x01,
        24 to 0x11, 25 to 0x11, 26 to 0x11, 27 to 0x11, 28 to 0x11,
        29 to 0x11, 30 to 0x11,
        
        31 to 0x12, 32 to 0x12, 33 to 0x12, 34 to 0x12, 35 to 0x12,
        36 to 0x22, 37 to 0x22, 38 to 0x22, 39 to 0x22, 40 to 0x22,
        
        41 to 0x23, 42 to 0x23, 43 to 0x23, 44 to 0x23, 45 to 0x23,
        46 to 0x23, 47 to 0x23, 48 to 0x33
    )
    
    fun getPixelBasedSize(pixelSize: Int): Int {
        return PIXEL_SIZE_MAP[pixelSize] ?: calculateDynamicSize(pixelSize)
    }
    
    private fun calculateDynamicSize(pixelSize: Int): Int {
        val baseFontWidth = 12.0
        val baseFontHeight = 24.0
        
        return when {
            pixelSize <= 18 -> 0x00
            pixelSize <= 24 -> 0x01
            pixelSize <= 30 -> 0x11
            pixelSize <= 36 -> 0x12
            pixelSize <= 42 -> 0x22
            pixelSize <= 48 -> 0x23
            pixelSize <= 54 -> 0x33
            pixelSize <= 60 -> 0x34
            else -> 0x44
        }
    }
    
    // Returns actual pixel dimensions (width, height)
    fun getActualPixelSize(multiplier: Int): Pair<Int, Int> {
        val widthMult = ((multiplier shr 4) and 0x0F) + 1
        val heightMult = (multiplier and 0x0F) + 1
        return Pair(12 * widthMult, 24 * heightMult)
    }
    
    fun getPrecisePixelSize(targetPixelSize: Int): Int {
        val baseWidth = 12
        val baseHeight = 24
        
        var bestMultiplier = 0x00
        var bestDifference = Int.MAX_VALUE
        
        for (w in 1..8) {
            for (h in 1..8) {
                val actualWidth = baseWidth * w
                val actualHeight = baseHeight * h
                val avgSize = (actualWidth + actualHeight) / 2
                val difference = kotlin.math.abs(avgSize - targetPixelSize)
                
                if (difference < bestDifference) {
                    bestDifference = difference
                    bestMultiplier = ((w - 1) shl 4) or (h - 1)
                }
            }
        }
        
        return bestMultiplier
    }
}