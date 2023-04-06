package net.djvk.inkyPhotoPrep.dithering

import net.djvk.inkyPhotoPrep.DisplayModel
import net.djvk.inkyPhotoPrep.model
import java.awt.Color
import java.awt.image.BufferedImage

//// Inline comments are output from inky/examples/7color/colour_palette.py
//// python3 colour-palette.py -f ~/palette -t gpl
//private val pal: Array<UInt> = arrayOf(
////    28 24 28 Index 0 # black
//    Color(28, 24, 28).rgb.toUInt(),
////    255 255 255 Index 1 # white
//    Color(255, 255, 255).rgb.toUInt(),
////    29 173 35 Index 2 # green
//    Color(29, 173, 35).rgb.toUInt(),
////    30 29 174 Index 3 # blue
//    Color(30, 29, 174).rgb.toUInt(),
////    205 36 37 Index 4 # red
//    Color(205, 36, 37).rgb.toUInt(),
////    231 222 35 Index 5 # yellow
//    Color(231, 222, 35).rgb.toUInt(),
////    216 123 36 Index 6 # orange
//    Color(216, 123, 36).rgb.toUInt(),
//)

interface Ditherer {
    fun dither(srcImage: BufferedImage): BufferedImage
}