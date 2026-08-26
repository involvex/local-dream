package com.involvex.localdreamchat.utils

/**
 * Expands packed RGB bytes (the backend's default raw payload format) into
 * ARGB ints ready for Bitmap.setPixels. Stops at whichever buffer ends first
 * so a short payload can never index out of bounds.
 */
fun rgbBytesToPixels(rgb: ByteArray, pixels: IntArray) {
    val count = minOf(pixels.size, rgb.size / 3)
    for (i in 0 until count) {
        val index = i * 3
        val r = rgb[index].toInt() and 0xFF
        val g = rgb[index + 1].toInt() and 0xFF
        val b = rgb[index + 2].toInt() and 0xFF
        pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
