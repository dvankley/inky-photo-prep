package net.djvk.inkyPhotoPrep.lib

object BinaryUtilities {
    /**
     * Returns the power of 2 that [input] is, i.e. the log base 2 of [input]
     * Uses only bit shifting
     * Returns null if [input] is not a power of 2
     */
    fun getPowerOfTwo(input: Int): Int? {
        var working = input
        var offset = 0
        var found = false
        while (!found) {
            if (0b1 and working == 0b1) {
                // We found a set bit
                found = true

            }
            // Shift the working set down
            working = (working shr 1)
            offset++
        }
        // If we found a bit set and there are any bits still set after shifting that bit
        //  off, then this number was not a power of 2
        if (working > 0) {
            return null
        }
        return offset - 1
    }
}