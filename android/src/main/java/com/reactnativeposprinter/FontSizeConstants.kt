package com.reactnativeposprinter

object FontSizeConstants {
    private val PIXEL_SIZE_MAP = mapOf(
        // Small sizes - progressive scaling
        8 to 0x00,   // 1x1 Normal (12x24)
        9 to 0x10,   // 2x1 width (24x24)
        10 to 0x10,  // 2x1 width (24x24)
        11 to 0x10,  // 2x1 width (24x24)
        12 to 0x11,  // 2x2 both (24x48)
        13 to 0x11,  // 2x2 both (24x48)
        14 to 0x11,  // 2x2 both (24x48)
        15 to 0x20,  // 3x1 width (36x24)
        16 to 0x20,  // 3x1 width (36x24)
        17 to 0x20,  // 3x1 width (36x24)
        18 to 0x21,  // 3x2 (36x48)
        
        // Medium sizes - balanced scaling
        19 to 0x21,  // 3x2 (36x48)
        20 to 0x21,  // 3x2 (36x48)
        21 to 0x22,  // 3x3 (36x72)
        22 to 0x22,  // 3x3 (36x72)
        23 to 0x22,  // 3x3 (36x72)
        24 to 0x22,  // 3x3 (36x72)
        25 to 0x30,  // 4x1 width (48x24)
        26 to 0x30,  // 4x1 width (48x24)
        27 to 0x30,  // 4x1 width (48x24)
        28 to 0x31,  // 4x2 (48x48)
        29 to 0x31,  // 4x2 (48x48)
        30 to 0x31,  // 4x2 (48x48)
        
        // Large sizes - progressive scaling
        31 to 0x32,  // 4x3 (48x72)
        32 to 0x32,  // 4x3 (48x72)
        33 to 0x32,  // 4x3 (48x72)
        34 to 0x33,  // 4x4 (48x96)
        35 to 0x33,  // 4x4 (48x96)
        36 to 0x33,  // 4x4 (48x96)
        37 to 0x40,  // 5x1 width (60x24)
        38 to 0x40,  // 5x1 width (60x24)
        39 to 0x40,  // 5x1 width (60x24)
        40 to 0x41,  // 5x2 (60x48)
        
        // Extra large sizes
        41 to 0x41,  // 5x2 (60x48)
        42 to 0x42,  // 5x3 (60x72)
        43 to 0x42,  // 5x3 (60x72)
        44 to 0x43,  // 5x4 (60x96)
        45 to 0x43,  // 5x4 (60x96)
        46 to 0x44,  // 5x5 (60x120)
        47 to 0x44,  // 5x5 (60x120)
        48 to 0x44   // 5x5 (60x120)
    )
    
    fun getPixelBasedSize(pixelSize: Int): Int {
        return PIXEL_SIZE_MAP[pixelSize] ?: calculateDynamicSize(pixelSize)
    }
    
    private fun calculateDynamicSize(pixelSize: Int): Int {
        return when {
            pixelSize <= 8 -> 0x00   // 1x1
            pixelSize <= 11 -> 0x10  // 2x1
            pixelSize <= 14 -> 0x11  // 2x2
            pixelSize <= 17 -> 0x20  // 3x1
            pixelSize <= 20 -> 0x21  // 3x2
            pixelSize <= 24 -> 0x22  // 3x3
            pixelSize <= 27 -> 0x30  // 4x1
            pixelSize <= 30 -> 0x31  // 4x2
            pixelSize <= 33 -> 0x32  // 4x3
            pixelSize <= 36 -> 0x33  // 4x4
            pixelSize <= 40 -> 0x40  // 5x1
            pixelSize <= 42 -> 0x41  // 5x2
            pixelSize <= 45 -> 0x42  // 5x3
            pixelSize <= 48 -> 0x43  // 5x4
            else -> 0x44            // 5x5
        }
    }
    
    // Returns actual pixel dimensions (width, height)
    fun getActualPixelSize(multiplier: Int): Pair<Int, Int> {
        val widthMult = ((multiplier shr 4) and 0x0F) + 1
        val heightMult = (multiplier and 0x0F) + 1
        return Pair(12 * widthMult, 24 * heightMult)
    }
}