package net.djvk.inkyPhotoPrep

import java.awt.image.BufferedImage

enum class Rotation {
    CLOCKWISE_90, CLOCKWISE_180, CLOCKWISE_270;

    fun rotate(original: BufferedImage): BufferedImage {
        val oW = original.width
        val oH = original.height
        val rotated = when (this) {
            CLOCKWISE_180 -> BufferedImage(oW, oH, original.type)
            else -> BufferedImage(oH, oW, original.type)
        }
        val rasterOriginal = original.copyData(null)
        val rasterRotated = rotated.copyData(null)
        /*
         * The Data for 1 Pixel...
         */
        val onePixel = IntArray(original.sampleModel.numBands)
        /*
         * Copy the Pixels one-by-one into the result...
         */for (x in 0 until oW) {
            for (y in 0 until oH) {
                rasterOriginal.getPixel(x, y, onePixel)
                when (this) {
                    CLOCKWISE_90 -> rasterRotated.setPixel(oH - 1 - y, x, onePixel)
                    CLOCKWISE_270 -> rasterRotated.setPixel(y, oW - 1 - x, onePixel)
                    else -> rasterRotated.setPixel(oW - 1 - x, oH - 1 - y, onePixel)
                }
            }
        }
        rotated.data = rasterRotated
        return rotated
    }
}