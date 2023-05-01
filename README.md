# ui for mizuki


## conclusion:

1. very mature, lazy column with dynamic row height, awesome component style(material 3), dark mode, rotation, status bar color, soft keyboard.
1. very hard for dynamic loading
    1. declarative ui toolkit doesn't work well with server driven mode, for control via jni => kotlin is the only lang, thus use dex loading
    1. aar/dex not bundle dependencies, fat-aar-pluging not support agp8 => have to bundle dependency manually
    1. dex class loader uses cached class, dex using a different version from apk is unsafe => dependency dead since apk born

## build

in android studio, set gradle jdk to 17

in build variant, select full
