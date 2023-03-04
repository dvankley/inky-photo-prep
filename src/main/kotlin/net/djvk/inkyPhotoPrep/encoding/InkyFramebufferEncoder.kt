package net.djvk.inkyPhotoPrep.encoding

import net.djvk.inkyPhotoPrep.lib.BinaryUtilities
import java.awt.Color
import java.awt.image.BufferedImage

/**
 * Based on https://github.com/pimoroni/pimoroni-pico/issues/681#issuecomment-1440469730
 */
class InkyFramebufferEncoder(
    private val width: Int,
    private val height: Int,
    private val palette: Array<Color>,
) {
    private val paletteRgb = palette.map { it.rgb }
    private val bitDepth = BinaryUtilities.getPowerOfTwo(palette.size)
        ?: throw IllegalArgumentException("Palette size is not a power of 2")
    private val bitPlaneSizeBits = width * height
    init {
        if (bitPlaneSizeBits % 8 != 0) {
            throw IllegalArgumentException("Bit plane size does not match byte boundaries. " +
                    "It should be doable, but I haven't accounted for this and odds are it means something is wrong.")
        }
    }

    private val bitPlaneSizeBytes = bitPlaneSizeBits / 8

    private val totalBits = bitPlaneSizeBits * bitDepth
    private val totalBytes = totalBits / 8

    fun encode(img: BufferedImage): ByteArray {
        val outputBuffer = ByteArray(totalBytes)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = img.getRGB(x, y)
                val pixelIndex = (y * width) + x
                val palIndex = paletteRgb.indexOf(pixel)

                for (depth in 0 until bitDepth) {
                    /**
                     * If the bit for this pixel in this bit plane should not be set, we don't need to do
                     *  anything here because our buffer is zeroed out by default.
                     */
                    if ((palIndex and (1 shl depth)) == 0) {
                        continue
                    }

                    /** Offset of the start of the relevant bit plane in the output buffer */
                    val bitPlaneByteOffset = (bitDepth - depth - 1) * bitPlaneSizeBytes
                    /** Offset of this pixel, in bytes, from the start of the relevant bit plane */
                    val pixelByteOffset = pixelIndex / 8
                    /**
                     * Offset of this pixel, in bits, from the start of the byte in the output buffer pointed to
                     *  by [pixelByteOffset]
                     */
                    val pixelBitOffset = pixelIndex % 8
                    val bufferByteOffset = bitPlaneByteOffset + pixelByteOffset

                    outputBuffer[bufferByteOffset] = (outputBuffer[bufferByteOffset].toInt() or (0b10000000 shr pixelBitOffset)).toByte()
                }
            }
        }

        return outputBuffer
    }
}