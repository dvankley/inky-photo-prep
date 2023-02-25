# inky-photo-prep
Preprocessor for photos to display on the Inky Impression eInk display

# Purpose
I made this thing to process a bunch of my photos for display on the Inky Impression 7 color
eInk display.

It does the following:
- Converts all HEIC files in the input directory to JPGs (also in the input directory)
  - Make sure none of your input files have weird filenames because I haven't bothered
  to escape any characters in the call to ImageMagick.
- For each photo:
  - Runs facial detection
  - Determines a center point for cropping, which is the center of all detected faces
  or just the center of the photo if no faces were detected.
  - Crops the photo according to the above to fit the target display.
  - Dithers the photo to match the defined color palette (Inky Impression by default)
  - Writes the resulting image to the output directory

# Installation and Running
You will need Java and Maven for this because I haven't bothered to upload a JAR anywhere.
You will also need ImageMagick installed for the HEIC conversion to work.

Run `mvn clean compile exec:java -Dexec.args="$input $output"` where input is the relative path
to your directory of input images, and output is the relative path to where you want your output
images.

# Configuration
I was too lazy to extract most config to a file or args. All parameters are constants
in code that you can diddle with. See the constants at the top of `Main.kt` and `net.djvk.inkyPhotoPrep.PatternDitherer` especially.

# FAQs
1. Why didn't you do this in Python, where face detection is easier to use
and there's already sample code from Pimoroni for dithering?
    1. No good reason. I usually prefer Kotlin and can generally find a Java
    library for any of my purposes, but that kind of backfired this time, 
    and I was too deep in the ol' sunk cost fallacy to change course.
2. Why is Maven installing a zillion versions of JavaCPP?
   1. Maven isn't great at architecture-specific dependencies, so it's just carpet bombing.
   See [here](https://github.com/bytedeco/javacv#downloads) for instructions on how to limit
   your downloads.