package net.djvk.inkyPhotoPrep.dithering

import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


/**
 * An implementation of various error diffusion dithering algorithm variants
 * Adapted from https://github.com/hbldh/hitherdither/blob/master/hitherdither/diffusion.py
 */
class ErrorDiffusion(
    private val palette: Array<Color>,
    private val algorithm: DiffusionMap,
) : Ditherer {
    private val colorDistance = ColorDistance(palette)

    data class DiffusionMapCoordinate(
        val x: Int,
        val y: Int,
        val value: Int,
    )

    /**
     * Various error diffusion algorithm maps
     * [@see http://bisqwit.iki.fi/jutut/kuvat/ordered_dither/error_diffusion.txt]
     * [@see https://tannerhelland.com/2012/12/28/dithering-eleven-algorithms-source-code.html]
     */
    enum class DiffusionMap(val map: List<DiffusionMapCoordinate>, val coefficientDivisor: Int) {
        FloydSteinberg(
            listOf(
                DiffusionMapCoordinate(1, 0, 7),
                DiffusionMapCoordinate(-1, 1, 3),
                DiffusionMapCoordinate(0, 1, 5),
                DiffusionMapCoordinate(1, 1, 1),
            ), 16
        ),
        Atkinson(
            listOf(
                DiffusionMapCoordinate(1, 0, 1),
                DiffusionMapCoordinate(2, 0, 1),
                DiffusionMapCoordinate(-1, 1, 1),
                DiffusionMapCoordinate(0, 1, 1),
                DiffusionMapCoordinate(1, 1, 1),
                DiffusionMapCoordinate(0, 2, 1),
            ), 8
        ),
        JarvisJudiceNinke(
            listOf(
                DiffusionMapCoordinate(1, 0, 7),
                DiffusionMapCoordinate(2, 0, 5),
                DiffusionMapCoordinate(-2, 1, 3),
                DiffusionMapCoordinate(-1, 1, 5),
                DiffusionMapCoordinate(0, 1, 7),
                DiffusionMapCoordinate(1, 1, 5),
                DiffusionMapCoordinate(2, 1, 3),
                DiffusionMapCoordinate(-2, 2, 1),
                DiffusionMapCoordinate(-1, 2, 3),
                DiffusionMapCoordinate(0, 2, 5),
                DiffusionMapCoordinate(1, 2, 3),
                DiffusionMapCoordinate(2, 2, 1),
            ), 48
        ),
        Stucki(
            listOf(
                DiffusionMapCoordinate(1, 0, 8),
                DiffusionMapCoordinate(2, 0, 4),
                DiffusionMapCoordinate(-2, 1, 2),
                DiffusionMapCoordinate(-1, 1, 4),
                DiffusionMapCoordinate(0, 1, 8),
                DiffusionMapCoordinate(1, 1, 4),
                DiffusionMapCoordinate(2, 1, 2),
                DiffusionMapCoordinate(-2, 2, 1),
                DiffusionMapCoordinate(-1, 2, 2),
                DiffusionMapCoordinate(0, 2, 4),
                DiffusionMapCoordinate(1, 2, 2),
                DiffusionMapCoordinate(2, 2, 1),
            ), 42
        ),
        Burkes(
            listOf(
                DiffusionMapCoordinate(1, 0, 8),
                DiffusionMapCoordinate(2, 0, 4),
                DiffusionMapCoordinate(-2, 1, 2),
                DiffusionMapCoordinate(-1, 1, 4),
                DiffusionMapCoordinate(0, 1, 8),
                DiffusionMapCoordinate(1, 1, 4),
                DiffusionMapCoordinate(2, 1, 2),
            ), 32
        ),
        Sierra3(
            listOf(
                DiffusionMapCoordinate(1, 0, 5),
                DiffusionMapCoordinate(2, 0, 3),
                DiffusionMapCoordinate(-2, 1, 2),
                DiffusionMapCoordinate(-1, 1, 4),
                DiffusionMapCoordinate(0, 1, 5),
                DiffusionMapCoordinate(1, 1, 4),
                DiffusionMapCoordinate(2, 1, 2),
                DiffusionMapCoordinate(-1, 2, 2),
                DiffusionMapCoordinate(0, 2, 3),
                DiffusionMapCoordinate(1, 2, 2),
            ), 32
        ),
        Sierra2(
            listOf(
                DiffusionMapCoordinate(1, 0, 4),
                DiffusionMapCoordinate(2, 0, 3),
                DiffusionMapCoordinate(-2, 1, 1),
                DiffusionMapCoordinate(-1, 1, 2),
                DiffusionMapCoordinate(0, 1, 3),
                DiffusionMapCoordinate(1, 1, 2),
                DiffusionMapCoordinate(2, 1, 1),
            ), 16
        ),
        Sierra_2_4a(
            listOf(
                DiffusionMapCoordinate(1, 0, 2),
                DiffusionMapCoordinate(-1, 1, 1),
                DiffusionMapCoordinate(0, 1, 1),
            ), 4
        ),
    }

    override fun dither(srcImage: BufferedImage): BufferedImage {
        val redBuffer = Array(srcImage.height) { Array(srcImage.width) { 0.0 }}
        val greenBuffer = Array(srcImage.height) { Array(srcImage.width) { 0.0 }}
        val blueBuffer = Array(srcImage.height) { Array(srcImage.width) { 0.0 }}
        // Init the destination buffers
        for (y in 0 until srcImage.height) {
            for (x in 0 until srcImage.width) {
                val pixel = srcImage.getRGB(x, y)
                redBuffer[y][x] = (pixel and 0xFF).toDouble()
                greenBuffer[y][x] = ((pixel shr 8) and 0xFF).toDouble()
                blueBuffer[y][x] = ((pixel shr 16) and 0xFF).toDouble()
            }
        }
        for (y in 0 until srcImage.height) {
            for (x in 0 until srcImage.width) {
                val oldRed = max(min(redBuffer[y][x], 255.0), 0.0)
                val oldGreen = max(min(greenBuffer[y][x], 255.0), 0.0)
                val oldBlue = max(min(blueBuffer[y][x], 255.0), 0.0)

                val old_pixel = Color(oldRed.roundToInt(), oldGreen.roundToInt(), oldBlue.roundToInt())
                val new_pixel = colorDistance.closestColor(old_pixel, ColorDistance::comparisonEuclidean)
                val quantErrorRed = oldRed - new_pixel.red
                val quantErrorGreen = oldGreen - new_pixel.green
                val quantErrorBlue = oldBlue - new_pixel.blue

                redBuffer[y][x] = new_pixel.red.toDouble()
                greenBuffer[y][x] = new_pixel.green.toDouble()
                blueBuffer[y][x] = new_pixel.blue.toDouble()

                for ((dx, dy, mapValue) in algorithm.map) {
                    val diffusionCoefficient = mapValue.toDouble() / algorithm.coefficientDivisor
                    val xn = x + dx
                    val yn = y + dy
                    if ((0 <= xn) &&
                        (xn < srcImage.width) &&
                        (0 <= yn) &&
                        (yn < srcImage.height)
                    ) {
                        redBuffer[yn][xn] += quantErrorRed * diffusionCoefficient
                        greenBuffer[yn][xn] += quantErrorGreen * diffusionCoefficient
                        blueBuffer[yn][xn] += quantErrorBlue * diffusionCoefficient
                    }
                }
            }
        }
        val out = BufferedImage(srcImage.width, srcImage.height, srcImage.type)
        for (y in 0 until srcImage.height) {
            for (x in 0 until srcImage.width) {
                out.setRGB(x, y, (blueBuffer[y][x].roundToInt() shl 16) + (greenBuffer[y][x].roundToInt() shl 8) + redBuffer[y][x].roundToInt())
            }
        }

        return out
    }
}