package net.djvk.inkyPhotoPrep.dithering

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

// From https://github.com/pimoroni/pimoroni-pico/issues/681#issuecomment-1440469730
val inkyPalette: Array<Color> = arrayOf(
    Color(0, 0, 0),
    Color(255, 255, 255),
    Color(0, 255, 0),
    Color(0, 0, 255),
    Color(255, 0, 0),
    Color(255, 255, 0),
    Color(255, 128, 0),
    Color(220, 180, 200),
)

interface Ditherer {
    fun dither(srcImage: BufferedImage): BufferedImage
}