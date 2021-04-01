# Subreddimages

Subreddimages allows you to create any image out of thousands images dowloaded from a subreddit of your choice!

# Requirements

### Python

*You need Python **>=3.4.0** to be installed*

### Dependencies

Before use, some python modules may need installing;

- [psaw](https://github.com/dmarx/psaw)
- [pillow](https://github.com/python-pillow/Pillow)
- [opencv-python](https://github.com/skvark/opencv-python)
- [numpy](https://github.com/numpy/numpy)
- [imageio](https://github.com/imageio/imageio) + imageio-ffmpeg
- [tifffile](https://github.com/cgohlke/tifffile/)
- [h5py](https://pypi.org/project/h5py/)

## Setup

To install all the libraries run:

```
pip install psaw pillow opencv-python numpy imageio imageio-ffmpeg tifffile h5py
```

### Application

* You can download the latest version of Subreddimages here:
  * [Subreddimages](https://github.com/DexterHill0/Subreddimages/releases/tag/v1.0.0)
* The Python script can also be run independently, [using command line arguments](https://github.com/DexterHill0/Subreddimages/blob/master/README.md#command-line).
* You can also [build from source](https://github.com/DexterHill0/Subreddimages/blob/master/README.md#building-from-source).

# Features

* Subreddit - The subreddit you want the images downloaded from.
  `--sub-name` / `-sub`

* Ouput directory - The place where the finished image will be saved.
  `--out-dir` / `-out` 

* Image to create - The image you want created.
  `--in-file` / `-in`

* Sort by - How the images on the subreddit should be sorted. 
  `--sort` / `-sort` (`pday`, `pweek`, `pmonth`, `pyear`, `alltime`)

* NSFW - Whether any images marked NSFW should be downloaded.<sup>[◊](https://github.com/DexterHill0/Subreddimages/blob/master/README.md#Extras)</sup>
  `--nsfw` / `-nsfw`

* Nearest by - `colour` will pick the nearest image by colour. `brightness` will pick the nearest image by brightness. <sup>[Δ](https://github.com/DexterHill0/Subreddimages/blob/master/README.md#Extras)</sup>
  `--nearest` / `-near`

* Image section size - The size the images should be cropped to. 
  `--size` / `-size`

* Maximum images - A cap on how many images should be retrieved from the subreddit.
  `--max-images` / `-max` 

  To run the program by command line, use the arguments shown. **All these arguments must be included**

# Building from source

* Download the source code:
  
  * [Source](https://github.com/DexterHill0/Subreddimages/archive/refs/heads/master.zip)
  
  ### Eclipse

* Download the latest Eclipse version [here](https://www.eclipse.org/downloads/)

* Open Eclipse and under package explorer:
  
  ```
  File -> Import -> General -> Existing projects into workspace -> Add Subreddimages to root directory -> Finish
  ```

* To export as a JAR:

* If you haven't yet run it, right click `MainGUI.java`: 
  
  ```
  Run as -> Java Application
  ```

  Then close it.
  
* Under package explorer, right click `Subreddimages`:
  
  ```
  Export -> Java -> Runnable JAR file -> Next -> Finish
                                       - Lanch configuration: MainGUI - Subreddimages
                                       - Package required libraries into generated JAR
  ```
  
* To run the jar, `cd` to the folder it is located in and run:

  `java -jar <name>.jar`

# Extras

### Notes

* <sup>◊</sup> NSFW will only work if the Reddit post was marked as NSFW.

* <sup>Δ</sup> `colour` will create a cloured image, `brightness` will create a black-and-white looking image.

* <sup>§</sup> `all time` does not mean `top of all time`. There is not currently a way to sort like that in psaw.

* To open the TIFF file produced, on Windows [IrfanView](https://www.irfanview.net/) seemed to work mostly consistently.
  
  On MacOS it seems [GraphicConverter](https://www.lemkesoft.de/en/products/graphicconverter/) was recommended although I never tried it.
  
  For Linux, [Gwenview](https://kde.org/applications/graphics/org.kde.gwenview) was recommended although, again, I never tried it.


