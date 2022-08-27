import java.awt.Color
import java.awt.image.BufferedImage

/**
 * Adapted from https://bisqwit.iki.fi/story/howto/dither/jy/
 *
 * I tried to find a library to do this straight up, but the best I could find was
 *  [org.apache.commons.imaging.palette.Dithering], which only thresholds an image to a set number of
 *  colors, it doesn't let you define the palette to dither to.
 * So I did some googling and I found the above writeup, which is very thorough, with some C++ sample
 *  code, which I converted to Kotlin and it mostly worked fine except for a teeny bug at the end
 *  that I fixed.
 * One side effect of this is there's a bunch of unsigned types in here that probably don't need to be
 *  here, I just left them in to minimize the chance of bugs in the conversion.
 */
class PatternDitherer {
    private val errorMultiplier = .09

    // Inline comments are output from inky/examples/7color/colour_palette.py
    // python3 colour-palette.py -f ~/palette -t gpl
    private val pal: Array<UInt> = arrayOf(
//    28 24 28 Index 0 # black
        Color(28, 24, 28).rgb.toUInt(),
//    255 255 255 Index 1 # white
        Color(255, 255, 255).rgb.toUInt(),
//    29 173 35 Index 2 # green
        Color(29, 173, 35).rgb.toUInt(),
//    30 29 174 Index 3 # blue
        Color(30, 29, 174).rgb.toUInt(),
//    205 36 37 Index 4 # red
        Color(205, 36, 37).rgb.toUInt(),
//    231 222 35 Index 5 # yellow
        Color(231, 222, 35).rgb.toUInt(),
//    216 123 36 Index 6 # orange
        Color(216, 123, 36).rgb.toUInt(),
    )

    /* 8x8 threshold map (note: the patented pattern dithering algorithm uses 4x4) */
    private val map: Array<UByte> = arrayOf(
        0u.toUByte(),48u.toUByte(),12u.toUByte(),60u.toUByte(), 3u.toUByte(),51u.toUByte(),15u.toUByte(),63u.toUByte(),
        32u.toUByte(),16u.toUByte(),44u.toUByte(),28u.toUByte(),35u.toUByte(),19u.toUByte(),47u.toUByte(),31u.toUByte(),
        8u.toUByte(),56u.toUByte(), 4u.toUByte(),52u.toUByte(),11u.toUByte(),59u.toUByte(), 7u.toUByte(),55u.toUByte(),
        40u.toUByte(),24u.toUByte(),36u.toUByte(),20u.toUByte(),43u.toUByte(),27u.toUByte(),39u.toUByte(),23u.toUByte(),
        2u.toUByte(),50u.toUByte(),14u.toUByte(),62u.toUByte(), 1u.toUByte(),49u.toUByte(),13u.toUByte(),61u.toUByte(),
        34u.toUByte(),18u.toUByte(),46u.toUByte(),30u.toUByte(),33u.toUByte(),17u.toUByte(),45u.toUByte(),29u.toUByte(),
        10u.toUByte(),58u.toUByte(), 6u.toUByte(),54u.toUByte(), 9u.toUByte(),57u.toUByte(), 5u.toUByte(),53u.toUByte(),
        42u.toUByte(),26u.toUByte(),38u.toUByte(),22u.toUByte(),41u.toUByte(),25u.toUByte(),37u.toUByte(),21u.toUByte()
    )


    /* Luminance for each palette entry, to be initialized as soon as the program begins */
    var luma = Array<UInt>(pal.size) { 0.toUInt() }

    private fun PaletteCompareLuma(index1: UInt, index2: UInt): Int
    {
        return (luma[index1.toInt()] - luma[index2.toInt()]).toInt()
    }

    private fun ColorCompare(color1: Color, color2: Color): Double
    {
        val luma1: Double = (color1.red*299 + color1.green*587 + color1.blue*114) / (255.0*1000)
        val luma2: Double = (color2.red*299 + color2.green*587 + color2.blue*114) / (255.0*1000)
        val lumadiff: Double = luma1-luma2
        val diffR = (color1.red-color2.red)/255.0
        val diffG = (color1.green-color2.green)/255.0
        val diffB = (color1.blue-color2.blue)/255.0
        return (diffR*diffR*0.299 + diffG*diffG*0.587 + diffB*diffB*0.114)*0.75 + lumadiff*lumadiff
    }

    private class MixingPlan {
        val colors = Array(64) { 0.toUInt() }
    }

    private fun DeviseBestMixingPlan(color: Int): MixingPlan
    {
        val result = MixingPlan()
        val src = Color(color)

        val x = errorMultiplier
        val e = arrayOf<Int>(0, 0, 0) // Error accumulator
        for(c in 0u until map.size.toUInt())
        {
            // Current temporary value
            val t = arrayOf<Int>(
                (src.red + e[0] * x).toInt(),
                (src.green + e[1] * x).toInt(),
                (src.blue + e[2] * x).toInt()
            )
            // Clamp it in the allowed RGB range
            if(t[0]<0) t[0]=0; else if(t[0]>255) t[0]=255
            if(t[1]<0) t[1]=0; else if(t[1]>255) t[1]=255
            if(t[2]<0) t[2]=0; else if(t[2]>255) t[2]=255
            // Find the closest color from the palette
            var leastPenalty: Double = 1e99
            var chosen: UInt = c % pal.size.toUInt()
            for(index in pal.indices)
            {
                val pc = Color(pal[index].toInt())
                val penalty: Double = ColorCompare(pc, Color(t[0],t[1],t[2]))
                if(penalty < leastPenalty)
                {
                    leastPenalty = penalty
                    chosen=index.toUInt()
                }
            }
            // Add it to candidates and update the error
            result.colors[c.toInt()] = chosen
            val pc = Color(pal[chosen.toInt()].toInt())
            e[0] += src.red-pc.red
            e[1] += src.green-pc.green
            e[2] += src.blue-pc.blue
        }
        // Sort the colors according to luminance
        result.colors.sortedWith(::PaletteCompareLuma)
        return result
    }

    fun dither(srcim: BufferedImage): BufferedImage {
        val w = srcim.width
        val h = srcim.height
        val im = BufferedImage(srcim.width, srcim.height, BufferedImage.TYPE_INT_RGB)
        for (c in 0u until pal.size.toUInt())
        {
            val r: UInt = pal[c.toInt()] shr 16
            val g: UInt = (pal[c.toInt()] shr 8) and 0xFFu
            val b: UInt = pal[c.toInt()] and 0xFFu
            luma[c.toInt()] = r*299u + g*587u + b*114u
        }
        for (y in 0 until h) {
            for (x in 0 until w)
            {
                val color = srcim.getRGB(x, y)
                val map_value: UInt = map [(x and 7)+((y and 7) shl 3)].toUInt()
                val plan = DeviseBestMixingPlan (color)
                // The code online was conspicuously missing the pal[] reference here, which breaks the whole thing
                im.setRGB(x, y, pal[plan.colors[map_value.toInt()].toInt()].toInt())
            }
        }
        return im
    }
}