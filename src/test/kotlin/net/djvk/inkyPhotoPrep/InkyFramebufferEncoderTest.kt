package net.djvk.inkyPhotoPrep

import net.djvk.inkyPhotoPrep.dithering.inkyPalette
import net.djvk.inkyPhotoPrep.encoding.InkyFramebufferEncoder
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import kotlin.test.assertContentEquals

@OptIn(ExperimentalUnsignedTypes::class)
internal class InkyFramebufferEncoderTest {
    @Test
    fun test_encode_simple() {
        val inputPixels = listOf(
            uintArrayOf(inkyPalette[4], inkyPalette[4], inkyPalette[4], inkyPalette[2]),
            uintArrayOf(inkyPalette[1], inkyPalette[1], inkyPalette[1], inkyPalette[2]),
        )
        val height = inputPixels.size
        val width = inputPixels.first().size

        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for ((y, line) in inputPixels.withIndex()) {
            for ((x, pixel) in line.withIndex()) {
                img.setRGB(x, y, pixel.toInt())
            }
        }

        val encoder = InkyFramebufferEncoder(width, height, inkyPalette)
        val actual = encoder.encode(img)

        val expected = byteArrayOf(
            // First bit plane, most significant bit in the palette index
            0b11100000.toByte(),
            // Second bit plane, middle bit in the palette index
            0b00010001,
            // Third bit plane, least significant bit in the palette index
            0b00001110,
        )

        assertContentEquals(expected, actual)
    }
}