package net.djvk.inkyPhotoPrep.encoding

import net.djvk.inkyPhotoPrep.lib.BinaryUtilities
import java.awt.image.BufferedImage

/**
 * Based on https://github.com/pimoroni/pimoroni-pico/issues/681#issuecomment-1440469730
 */
class InkyFramebufferEncoder(
    val width: Int,
    val height: Int,
    val palette: Array<UInt>,
) {
    private val bitDepth = BinaryUtilities.getPowerOfTwo(palette.size)
        ?: throw IllegalArgumentException("Palette size is not a power of 2")
    private val totalBits = width * height * bitDepth
    private val totalBytes = totalBits / 8

    fun encode(img: BufferedImage): ByteArray {

        return ByteArray(1)
    }
}