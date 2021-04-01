from psaw import PushshiftAPI
from pathlib import Path
import operator
import requests
import argparse
import urllib
import time
import json
import glob
import h5py
import gc
import os
import re

# IMAGE PROCESSING
from PIL import Image, ImageFile
import numpy as np
import tifffile
import imageio
import cv2

rgb, links = [], []

_VERSION = "1.0.0"


# allows you to clear the contents of any file or directory
def _clear_contents(file_path):
    if os.path.isfile(file_path):
        with open(file_path, 'w') as clear:
            clear.write('')
    elif not os.path.isfile(file_path):
        files = _get_all_files(file_path)
        for i in files:
            os.remove(os.path.join(os.path.dirname(file_path), i))


def _get_all_files(sub_dir):
    return glob.glob(str(sub_dir))


# decorator to calculate duration taken by any function because why not
def _calculate_time(func):
    def inner1(*args, **kwargs):
        begin = time.time()
        func(*args, **kwargs)
        end = time.time()
        _.send("Took: {}s to complete function: '{}'".format(
            end-begin, func.__name__), CONST.INFO)

    return inner1


def _counter(func):  # counts how many times function was called
    def inner1(*args, **kwargs):
        inner1.calls += 1
        return func(*args, **kwargs)
    inner1.calls = 0
    inner1.__name__ = func.__name__

    return inner1


def _find_closest(nearest, col, rgb):
    if(nearest.lower()[0] == 'c'):
        return rgb[np.argmin(np.abs(rgb - col).sum(axis=1))]
    elif(nearest.lower()[0] == 'b'):
        return rgb[np.argmin(np.abs(rgb.sum(axis=1) - col.sum()))]


class _():
    def send(msg, _type):
        # prints to console since the java app reads from the console output
        print("{} {}".format(CONST._LEVELS[_type], msg))


class CONST():
    # valid suffixes
    _SUFFIXES = ["jpeg", "jpg", "png", "gif", "mp4", "m3u8", "gifv"]

    # current working directory (current path)
    _DIR_PATH = Path(os.path.realpath(__file__).replace(
        os.path.basename(__file__), ""))

    # valid imgur links
    _IMGUR_URL = re.compile(
        r'https?://(?:i\.)?imgur\.com/(?:(r/pics|a|)/?)\b(?!gallery|t)\w*\b(?P<id>[a-zA-Z0-9]+)')
    _IMGUR_GALLERY_URL = re.compile(
        r'https?://imgur\.com/(?:gallery|(?:t)/[^/]+)/(?P<id>[a-zA-Z0-9]+)')

    # valid gfycat links
    _GFYCAT_URL = re.compile(
        r'https?://(?:(?:www|giant|thumbs)\.)?gfycat\.com/(?:ru/|ifr/|gifs/detail/)?(?P<id>[^-/?#\.]+)')

    # valid reddit links
    _VREDDIT_URL = re.compile(r'https?://v\.redd\.it/( ?P<id>[^/?#&]+)')
    _REDDIT_URL = re.compile(
        r'(?P<url>https?://(?:[^/]+\.)?reddit\.com/r/[^/]+/comments/(?P<id>[^/?#&]+))')
    _IREDDIT_URL = re.compile(r'https?://i.redd\.it/(?P<id>[^/?#&]+)\.')

    # valid giphy links
    _GIPHY_URL = re.compile(
        r'(^https?://(?:(?:i|media|media.)\.)?giphy\.com/(?:media|gifs)?/.*)(?P<id>(?<=-(?!.*-)).*(?=)|(?<=/).*(?=/))')
    _GIPHY_STORY_URL = re.compile(
        r'(https?://giphy\.com/stories)/(?P<id>[a-zA-Z0-9-]+)')

    # valid discord link
    _DISCORD = re.compile(r'https?://(?:cdn|media)?\.discordapp\.(?:net|com)?')

    INFO = 1
    WARN = 2
    ERR = 3
    SEVERE = 4
    _LEVELS = {1: "[INFO]", 2: "[WARN]", 3: "[ERR]", 4: "END"}


class Setup():
    _OPTIONS = {}

    def setup(self):
        ImageFile.LOAD_TRUNCATED_IMAGES = True
        Image.MAX_IMAGE_PIXELS = float('inf')

        # create a argument parser to recieve arguments parsed in at terminal
        parser = argparse.ArgumentParser(
            description='Main imaging script for Subreddimages')

        # add arguments
        parser.add_argument('--max-images', '-max', type=int,
                            help='Maximum number of images', dest='maxs', default=None)
        parser.add_argument('--sub-name', '-sub', type=str,
                            help='Subreddit', dest='sub', default=None)
        parser.add_argument('--nsfw', '-nsfw', type=str,
                            help='Allow NSFW?', dest='nsfw', default=None)
        parser.add_argument('--nearest', '-near', type=str,
                            help='How the images should be picked', dest='near', default=None)
        parser.add_argument('--sort', '-sort', type=str,
                            help='How the posts should be sorted', dest='sort', default=None)
        parser.add_argument('--size', '-size', type=int,
                            help='Cropped size of the downloaded images', dest='size', default=None)
        parser.add_argument('--in-file', '-in', type=str,
                            help='The image to be created', dest='to_create', default=None)
        parser.add_argument('--out-dir', '-out', type=str,
                            help='Output directory', dest='out_dir', default=None)

        # parse arguemnts, convert to dictionary
        vargs = vars(parser.parse_args())
        args = parser.parse_args()  # parses arguments

        if (None in [arg for arg in vargs.values()]):
            _.send("Argument not supplied! {}".format([vars(x)["option_strings"][0] for x in parser.__dict__["_actions"] if type(x) == argparse._StoreAction and vars(
                args)[vars(x)["dest"]] == None]), CONST.SEVERE)  # checks if any of the arguments were not supplied - if not it shows all the arguments you didnt provide
            raise Exception
        if (not args.size % 2 == 0):
            # making size a multiple of 2 means it can be split in half nicely
            _.send("Size must be multiple of 2!", CONST.SEVERE)
            raise Exception
        if (args.size > 400):
            _.send("Not recomended to have crop size greater than 400!",
                   CONST.WARN)  # large crop size slow process a lot
            raise Exception
        if (not os.path.isfile(args.to_create) or not os.path.isdir(args.out_dir)):
            # validating the input image and output directory
            _.send("Input image not found / output directory invalid!", CONST.SEVERE)
            raise Exception

        # make image folder if it doesnt exist already
        Path(CONST._DIR_PATH / Path("images")
             ).mkdir(parents=True, exist_ok=True)

        Setup._OPTIONS = {  # make all args into a dict
            'max_images': args.maxs,
            'sub': args.sub,
            # converts string to bool
            'nsfw': json.loads(args.nsfw.lower()),
            'near': args.near,
            'sort': args.sort,
            'size': args.size,
            'to_create': args.to_create,
            'out': args.out_dir
        }


class Links(Setup):  # "inherit" the options from setup class - means 'options' still within it's own function (not set multiple times), no global variables needed, no returning functions needed
    @classmethod
    @_calculate_time  # using decorator
    def main(cls):
        _.send("Downloading images - may take a while", CONST.INFO)

        if (cls._OPTIONS['sort'].lower() == 'pday'):
            after_epoch = int(time.time() - 86400)  # current time minus 1 day
        elif (cls._OPTIONS['sort'].lower() == 'pweek'):
            # current time minus 1 week
            after_epoch = int(time.time() - 604800)
        elif (cls._OPTIONS['sort'].lower() == 'pmonth'):
            # current time minus 1 month
            after_epoch = int(time.time() - 2678400)
        elif (cls._OPTIONS['sort'].lower() == 'pyear'):
            # current time minus 1 year
            after_epoch = int(time.time() - 31536000)
        else:
            after_epoch = 0  # after the beginning of epoch time

        api = PushshiftAPI()
        SUBMISSIONS = api.search_submissions(subreddit=cls._OPTIONS['sub'],
                                             filter=['url', 'over_18'],
                                             limit=cls._OPTIONS['max_images'],
                                             after=after_epoch)

        try:  # this is to check whether the subreddit actually exists
            na = next(SUBMISSIONS)  # try get next element in generator
        except StopIteration as e:  # if it is empty it will error
            _.send("Subreddit does not exist! ({})".format(type(e)), CONST.SEVERE)
            raise Exception

        # get all the urls and put in the array if they are 'opposite' to nsfw. (ie if 'over_18' = True and 'allow_nsfw' = False, True + False = True, so it isn't added to the array)
        urls = [subs.d_['url'] for subs in SUBMISSIONS if (
                subs.d_['over_18'] and cls._OPTIONS['nsfw']) != True]
        for url in urls:
            Links.download_image(Links.parse_link(url))

        _.send("Downloaded images!", CONST.INFO)

    def parse_link(link):
        if (CONST._IMGUR_URL.match(link)):
            return Links.Imgur.imgur_url(CONST._IMGUR_URL.match(link))
        elif (CONST._IMGUR_GALLERY_URL.match(link)):
            return Links.Imgur.gallery_url(CONST._IMGUR_GALLERY_URL.match(link))
        elif (CONST._GFYCAT_URL.match(link)):
            return Links.Gfycat.gfycat(CONST._GFYCAT_URL.match(link))
        elif (CONST._GIPHY_URL.match(link)):
            return Links.Giphy.giphy_url(CONST._GIPHY_URL.match(link))
        elif (CONST._GIPHY_STORY_URL.match(link)):
            return Links.Giphy.giphy_story(CONST._GIPHY_STORY_URL.match(link))
        elif (CONST._VREDDIT_URL.match(link)):
            return Links.Reddit.v_reddit(CONST._VREDDIT_URL.match(link))
        elif (CONST._REDDIT_URL.match(link)):
            return Links.Reddit.reddit_url(CONST._REDDIT_URL.match(link))
        elif (CONST._IREDDIT_URL.match(link)):
            return link  # i.redd.it is already a direct link
        elif (CONST._DISCORD.match(link)):
            return link  # link is also already direct
        else:
            return ""

    @_counter
    def download_image(link):
        if (link != ""):
            if (isinstance(link, list)):  # images from a imgur gallery / giphy story come as a list
                for l in link:
                    Links.download_image(l)
                return
            suf = link.split(".")[-1]
            # This if statment will be true if for instace, it is a reddit text post.
            if (suf not in CONST._SUFFIXES):
                return
            try:
                # download the image - name it depending on number of times function was called
                urllib.request.urlretrieve(
                    link, "{}/{}.{}".format(CONST._DIR_PATH / Path("images"), Links.download_image.calls, suf))
            # the 'OSError: [Errno 0] Error' is really odd - not sure what causes it, not much info online either - seems like something to do with SSL??
            except (urllib.error.HTTPError, urllib.error.ContentTooShortError, OSError, urllib.error.URLError) as e:
                if (e == OSError):
                    _.send("Error while downloading: ({})".format(e), CONST.ERR)

    class Imgur():
        def imgur_url(parsed):
            imgur_id = parsed.group("id")

            webpage = requests.get(
                "https://i.imgur.com/{}.gifv".format(imgur_id)).text

            try:
                container = re.search(
                    r'(?s)<div class="post-image">(.*?)</div>', webpage).group(0)
            except AttributeError:
                return ""

            img = re.compile(r'<img\s+src=\"(?P<src>[^\"]+)\"')
            vid = re.compile(r'<source\s+src=\"(?P<src>[^\"]+)\"')

            is_img = re.search(img, container)
            if (is_img):
                return "https:{}".format(is_img.group("src"))

            is_vid = re.search(vid, container)
            if (is_vid):
                return "https:{}".format(is_vid.group("src"))

        def gallery_url(parsed):
            gallery_id = parsed.group("id")

            try:
                data = requests.get("https://imgur.com/gallery/{}.json".format(
                    gallery_id)).json()["data"]["image"]["album_images"]["images"]
            except Exception as e:
                _.send("Could not get gallery ({})".format(e), CONST.ERR)
                return ""
            images = ["https://i.imgur.com/{}{}".format(
                data[i]["hash"], data[i]["ext"]) for i in range(len(data))]

            return images

    class Gfycat():
        def gfycat(parsed):
            video_id = parsed.group("id")

            try:
                url = requests.get(
                    "https://api.gfycat.com/v1/gfycats/{}".format(video_id)).json()["gfyItem"]["gifUrl"]
            except Exception as e:
                _.send("Could not get gif ({})".format(e), CONST.ERR)
                return ""

            return url

    class Giphy():
        def giphy_url(parsed):
            video_id = parsed.group("id")

            return "https://i.giphy.com/media/{}/giphy.gif".format(video_id)

        def giphy_story(parsed):
            story_id = parsed.group("id")

            try:
                # NOT MY API KEY -- seems to be the one that giphy uses to get data on story (maybe 'developer' api key?)
                data = requests.get(
                    "https://x.giphy.com/v1/stories/slug/{}?api_key=3eFQvabDx69SMoOemSPiYfh9FY0nzO9x".format(story_id)).json()["data"]["gifs"]
            except Exception as e:
                _.send("Could not get story ({})".format(e), CONST.ERR)
                return ""

            images = ["https://i.giphy.com/media/{}/giphy.gif".format(
                data[i]["gif"]["id"]) for i in range(len(data))]

            return images

    class Reddit():
        def v_reddit(parsed):
            video_id = parsed.group("id")

            resp = requests.get(
                "https://v.redd.it/{}/HLSPlaylist.m3u8".format(video_id))

            if (resp.status_code == 200):
                return "https://v.redd.it/{}/HLSPlaylist.m3u8".format(video_id)

        def reddit_url(parsed):
            url = parsed.group("url")

            try:
                req = urllib.request.Request(
                    "{}/.json".format(url),
                    data=None,
                    # Reddit wants me to use a custom user agent? Don't change I guess?
                    headers={
                        'User-Agent': 'any:com.dexterhill.subreddimages:{} (by /u/DreamingInsanity)'.format(_VERSION)}
                )
                data = json.loads(urllib.request.urlopen(req).read().decode(
                    "utf-8"))[0]['data']['children'][0]['data']
            except Exception as e:
                _.send("Could not get image ({})".format(e), CONST.ERR)
                return ""

            return data['url']


class Processing(Setup):
    _IMAGES = []

    @classmethod
    @_calculate_time
    def main(cls):
        _.send("Processing images", CONST.INFO)
        # gets all the paths to the images
        allfiles = _get_all_files(CONST._DIR_PATH/Path("images/*"))

        for file in allfiles:
            Processing.crop(file)

        _.send("Successfully processed images!", CONST.INFO)

    def parse_image(image):
        img_size = os.stat(image).st_size  # get size of image in bytes
        if (img_size <= 20):
            # remove image if size is below threshold (some images may be downloaded but be 0 bytes)
            os.remove(image)
            return None
        try:
            img = cv2.imread(image, cv2.IMREAD_UNCHANGED)
            if(img.any() == None):
                raise Exception

            return img, image
        except Exception:
            try:
                name = image.replace(".gif", ".png")
                data = imageio.get_reader(image).get_data(0)[:, :, :]
                cv2.imwrite(name, cv2.cvtColor(data, cv2.COLOR_BGR2RGB))

                return cv2.imread(name, cv2.IMREAD_UNCHANGED), name
            except Exception:
                os.remove(image)
                return None, image

    @classmethod
    def crop(cls, image):
        img, path = Processing.parse_image(image)  # parse new image
        try:
            if (img.any() == None):
                raise Exception
        except Exception as e:
            _.send("Could not crop image ({})!".format(e), CONST.ERR)
            os.remove(image)
            return

        os.remove(image)  # remove old image

        bounding = (cls._OPTIONS['size'], cls._OPTIONS['size'])

        if (img.shape[0] < bounding[0] or img.shape[1] < bounding[1]):
            _.send("Image too small to crop!", CONST.ERR)
            os.remove(image)
            return

        # cropping the image from the centre, using numpy
        start = tuple(map(lambda a, da: a//2-da//2, img.shape, bounding))
        end = tuple(map(operator.add, start, bounding))
        slices = tuple(map(slice, start, end))

        new_name = image.split('.')[0]+'.png'  # create new name for image
        cv2.imwrite(new_name, img[slices])  # save the new image

        img = cv2.imread(new_name, cv2.IMREAD_UNCHANGED)

        # after cropping, get the average
        Processing.get_average(img, new_name)

    def get_average(image, path):
        # image is 2d array, so get the average of the rows
        avg_col_per_row = np.average(image, axis=0)
        # then get the average of all the averages
        avg = np.average(avg_col_per_row, axis=0)

        try:
            rgb.append(avg.astype(int)[:3].tolist())
        except IndexError:  # error occurs in the rare off chance that the image is real B+W
            os.remove(path)
            return
        links.append(path)


# dont need to inherit 'setup' since 'processing' already inherits 'setup'
class MakeImage(Processing):

    @classmethod
    @_calculate_time
    def main(cls):
        suffix = Path(cls._OPTIONS['to_create']).suffix  # get suffix of gif
        if (suffix == 'gif'):
            _.send("Cannot create a gif - please input image!", CONST.ERR)
            raise Exception
        else:
            MakeImage.create_image()

    def add_alpha_channel(img):  # can add alpha channel to jpeg's
        try:
            b_channel, g_channel, r_channel = cv2.split(
                img)  # split rgb channels
            # creating a dummy alpha channel image
            alpha_channel = np.ones(
                b_channel.shape, dtype=b_channel.dtype) * 255
            # merging the two together
            img = cv2.merge((b_channel, g_channel, r_channel, alpha_channel))
        except Exception:
            pass
        return img

    @classmethod
    def create_image(cls):
        x, y = 0, 0
        isDisk = False
        hasTransparency = False

        _.send("Creating image - could take a *long* time!", CONST.INFO)

        to_make = cv2.imread(cls._OPTIONS['to_create'])

        if (to_make.shape[2] == 4):  # if it has transparency
            hasTransparency = True
            # add an alpha channel to the image
            to_make = MakeImage.add_alpha_channel(
                to_make, cv2.IMREAD_UNCHANGED)

        colours = to_make[:, :, :3]  # separate colour and alpha channels
        if (hasTransparency):
            alpha = to_make[:, :, 3:]

        # if it has transparency the array is going to have a 'depth' of 4
        depth = 4 if (hasTransparency) else 3

        new_img = None

        try:
            # trying to create the numpy array, if the array is too big for memory, it with throw a MemoryError
            new_img = np.zeros(
                (to_make.shape[0]*cls._OPTIONS['size'], to_make.shape[1]*cls._OPTIONS['size'], depth), dtype=np.uint8)
        except MemoryError as e:
            _.send(
                "Image is too big for ram, using disk (expect it to take much longer) - ({})".format(type(e)), CONST.WARN)
            isDisk = True  # isDisk is then set to true, so now the numpy array is stored on disk, not ram

        # rather than putting the image on ram (takes a LOT of ram), it's stored in a  file on disk and porcessing happens on there (a bit slower but more efficient at the same time)
        f = h5py.File(str(CONST._DIR_PATH/Path("data")), 'w')

        if (isDisk):
            # create a dataset witha an empty numpy array (compress with gzip since thats the best - not the fastest though) if the image is too big for ram
            new_img = f.create_dataset(
                "IMAGE", (to_make.shape[0]*cls._OPTIONS['size'], to_make.shape[1]*cls._OPTIONS['size'], depth), dtype=np.uint8, compression='gzip')

        for x in range(0, to_make.shape[0]):  # loop over x,y
            for y in range(0, to_make.shape[1]):
                closest = _find_closest(cls._OPTIONS['near'], np.array(
                    colours[x, y]), np.array(rgb)).tolist()  # find closest matching image to RGB
                # get image directory and open it and add alpha channel
                img = cv2.cvtColor(cv2.imread(
                    links[rgb.index(closest)]), cv2.COLOR_RGB2BGR)
                if (hasTransparency):
                    img = MakeImage.add_alpha_channel(img)
                x_offset = x*cls._OPTIONS['size']  # set paste positions
                y_offset = y*cls._OPTIONS['size']
                # overwrite parts of the array to the RGB values of the image (then set opacting with a number between 0 and 1 (255/255 = 1, 0/255 = 0))
                new_img[x_offset:x_offset+img.shape[0], y_offset:y_offset+img.shape[1]
                        ] = img * (alpha[x, y][0] / 255) if (hasTransparency) else img[:, :, :3]

        Save.save(
            str(Path("{}/out.tiff".format(cls._OPTIONS['out']))), new_img)

        f.close()
        # remove the file since it takes up a sizeable chunk of disk space
        os.remove(str(CONST._DIR_PATH/Path("data")))


class Save(Setup):  # custom TIFF save function

    def save(path, img):
        tileshape = (1024, 1024)

        # most programs don't support TIFFs with large strip sizes, so it's written in tiles instead
        def tile_generator(data, tileshape):
            for y in range(0, data.shape[0], tileshape[0]):
                for x in range(0, data.shape[1], tileshape[1]):
                    tile = data[y: y+tileshape[0], x: x+tileshape[1], :]
                    if (tile.shape[:2] != tileshape):
                        pad = (
                            (0, tileshape[0] - tile.shape[0]),
                            (0, tileshape[1] - tile.shape[1]),
                            (0, 0)
                        )
                        tile = np.pad(tile, pad, 'constant')
                    yield tile

        tifffile.imwrite(
            path,
            tile_generator(img, tileshape),
            dtype=np.uint8,
            shape=img.shape,
            tile=tileshape,
            bigtiff=True,
            compress='jpeg'
        )


if __name__ == '__main__':
    Setup().setup()
    _clear_contents(str(CONST._DIR_PATH)/Path('images/*'))
    Links.main()
    Processing.main()
    MakeImage.main()
    # clear once finished
    _clear_contents(str(CONST._DIR_PATH)/Path('images/*'))
