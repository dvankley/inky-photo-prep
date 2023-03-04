package net.djvk.inkyPhotoPrep.dithering

import com.github.ajalt.colormath.model.RGB
import java.awt.Color
import kotlin.math.pow
import kotlin.math.sqrt

class ColorDistance(
    private val palette: Array<Color>,
) {
    private val paletteColormath = palette.map { RGB(it.red, it.green, it.blue) }
    fun closestColor(source: Color, comparisonFunction: (a: Color, b: Color) -> Double): Color {
        val withDistance = palette.map { Pair(it, comparisonFunction(source, it)) }
        val sorted = withDistance.sortedBy { it.second }
        return sorted.first().first
    }

    companion object {
        fun comparisonEuclidean(a: Color, b: Color): Double {
            return sqrt(
                (a.red - b.red).toDouble().pow(2) +
                        (a.green - b.green).toDouble().pow(2) +
                        (a.blue - b.blue).toDouble().pow(2)
            )
        }

        fun comparisonRiemersmaRedmean(a: Color, b: Color): Double {
            val rMean = (a.red.toDouble() + b.red.toDouble()) / 2
            val deltaR = a.red.toDouble() - b.red.toDouble()
            val deltaG = a.green.toDouble() - b.green.toDouble()
            val deltaB = a.blue.toDouble() - b.blue.toDouble()
            return sqrt(
                (2 + rMean / 256) * deltaR.pow(2) +
                        4 * deltaG.pow(2) +
                        (2 + ((255 - rMean) / 256)) * deltaB.pow(2)
            )
        }
    }
}