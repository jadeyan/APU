/**
 * Copyright � 2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.mtk.j2me.graphics;


import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import net.cp.mtk.common.ArraySegment;
import net.cp.mtk.common.graphics.JpegDecoder;


public abstract class GraphicUtils
{
    /** Indicates that no scaling should be performed. */
    public static final int SCALE_NONE =    0;
    
    /** 
     * Indicates that smooth scaling should be performed.
     * 
     * This gives a better quality image and supports transparency, but requires more memory. 
     */
    public static final int SCALE_SMOOTH =  1;

    /** 
     * Indicates that fast scaling should be performed.
     * 
     * This is faster and uses less memory, but gives a poorer quality image and doesn't support transparency. 
     */
    public static final int SCALE_FAST =    2;

    
    private GraphicUtils()
    {
        super();
    }
    

    /**
     * Returns the location of the embedded thumbnail image in the specified JPEG image data, or null if no thumbnail was found.
     * 
     * @param imageData the JPEG encoded image data to search. May be null or empty.
     * @param offset    the index of the first byte to examine. Must be >=0 and < imageData.length.
     * @param length    the maximum number of bytes to examine, or 0 to examine all data.
     * @return the array segment containing the embedded thumbnail image, or null if no thumbnail could be found.
     */
    public static ArraySegment findEmbeddedThumbnail(byte[] imageData, int offset, int length)
    {
        try
        {
            return JpegDecoder.findEmbeddedThumbnail(imageData, offset, length);
        }
        catch (Throwable e)
        {
            return null;
        }
    }

    /**
     * Returns the embedded thumbnail image from the specified JPEG image data, or null if no thumbnail was found.
     * 
     * @param imageData the JPEG encoded image data to search. May be null or empty.
     * @param offset    the index of the first byte to examine. Must be >=0 and < imageData.length.
     * @param length    the maximum number of bytes to examine, or 0 to examine all data.
     * @return the embedded thumbnail image, or null if no thumbnail could be extracted.
     */
    public static Image getEmbeddedThumbnail(byte[] imageData, int offset, int length)
    {
        try
        {
            ArraySegment thumbnailSegment = JpegDecoder.findEmbeddedThumbnail(imageData, offset, length);
            if (thumbnailSegment == null)
                return null;
            
            //create an image from the specified thumbnail
            int thumbStartIndex = thumbnailSegment.getStartIndex();
            int thumbEndIndex = thumbnailSegment.getEndIndex();
            return Image.createImage(imageData, thumbStartIndex, (thumbEndIndex - thumbStartIndex));
        }
        catch (Throwable e)
        {
            return null;
        }
    }
    

    /** 
     * Resizes the specified image so that its height and width doesn't exceed the specified targets.
     * 
     * The specified image will not be resized if it is already smaller than the specified target size. If the image 
     * does need to be resized, its original aspect ratio will be maintained.
     *  
     * @param image     the image that should be resized.
     * @param maxWidth  the maximum width of the image, or -1 to leave the width unchanged.
     * @param maxHeight the maximum height of the image, or -1 to leave the height unchanged.
     * @param scaleType the type of scaling to perform when resizing the image. Must be one of GraphicUtils.SCALE_XXX.
     * @return the image which has been resized so that it doesn't exceed the specified maximum size.
     * @throws IllegalArgumentException if an unknown scale type was specified.
     */
    public static Image resizeImage(Image image, int maxWidth, int maxHeight, int scaleType)
    {
        if ( (image == null) || ((maxWidth <= 0) && (maxHeight <= 0)) || (scaleType == SCALE_NONE))
            return image;
            
        int sourceWidth = image.getWidth();
        int sourceHeight = image.getHeight();
        
        //determine the requested size
        int targetWidth = (maxWidth <= 0) ? sourceWidth : maxWidth;
        int targetHeight = (maxHeight <= 0) ? sourceHeight : maxHeight;
        
        //if the image is already smaller than the requested size, just return it as-is
        if ( (sourceWidth <= targetWidth) && (sourceHeight <= targetHeight) )
            return image;
        
        //make sure we retain the original aspect ratio
        if ((sourceWidth - targetWidth) > (sourceHeight - targetHeight))
            targetHeight = (targetWidth * sourceHeight) / sourceWidth;
        else
            targetWidth = (targetHeight * sourceWidth) / sourceHeight;
        
        //make sure the target size is reasonable
        if (targetHeight <= 0)
            targetHeight = 1;
        if (targetWidth <= 0)
            targetWidth = 1;
        
        if (scaleType == SCALE_SMOOTH)
            return scaleImageSmooth(image, sourceWidth, sourceHeight, targetWidth, targetHeight);
        else if (scaleType == SCALE_FAST)
            return scaleImageFast(image, sourceWidth, sourceHeight, targetWidth, targetHeight);
        else
            throw new IllegalArgumentException("Unknown scale type '" + scaleType + "' specified");
    }

    /* 
     * Resizes the specified image to the specified target size - doesn't require much memory but doesn't work for 
     * images with transparent pixels. 
     */
    private static Image scaleImageFast(Image image, int sourceWidth, int sourceHeight, int targetWidth, int targetHeight)
    {
        //resize the image one pixel at a time
        int dy, dx;
        Image scaledImage = Image.createImage(targetWidth, targetHeight);
        Graphics g = scaledImage.getGraphics();
        for (int y = 0; y < targetHeight; y++)
        {
            dy = (y * sourceHeight) / targetHeight;
            for (int x = 0; x < targetWidth; x++)
            {
                dx = (x * sourceWidth) / targetWidth;
                
                //draw one pixel of the original image into the scaled image - negative dx offsets image so desired 
                //pixel is at image(x,y) - the desired pixel is every "Nth" one, where N is a function of dx and dy.
                g.setClip(x, y, 1, 1);
                g.drawImage(image, x - dx, y - dy, Graphics.LEFT | Graphics.TOP);
            }
        }
        
        //return an immutable image based on the resized image
        return Image.createImage(scaledImage);
    }

    /* 
     * Resizes the specified image to the specified target size - requires more memory but does work for images with 
     * transparent pixels and gives a better quality image. 
     */
    private static Image scaleImageSmooth(Image image, int sourceWidth, int sourceHeight, int targetWidth, int targetHeight)
    {
        //get the pixels of the original image  
        int originalPixels[] = new int[sourceWidth * sourceHeight];  
        image.getRGB(originalPixels, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight);  
        
        //resize the original image (using smooth scaling)
        int resizedPixels[] = new int[targetWidth * targetHeight];
        int pixel, index, dx, dy, py, px, ny, nx, avgr, avgg, avgb;
        for (int y = 0; y < targetHeight; y++)
        {
            dy = y * sourceHeight / targetHeight;
            py = (dy < (sourceHeight - 1)) ? 1 : 0;
            for (int x = 0; x < targetWidth; x++)
            {
                dx = x * sourceWidth / targetWidth;
                index = y * targetWidth + x;
                pixel = originalPixels[dy * sourceWidth + dx];
                if (pixel >> 24 != 0)
                {
                    //re-sample the pixel to smooth the scaling
                    px = dx < (sourceWidth - 1) ? 1 : 0;
                    ny = dy > 0 ? 1 : 0;
                    nx = dx > 0 ? 1 : 0;
                    pixel = originalPixels[((dy - ny) * sourceWidth) + dx];
                    avgr = (pixel >>> 16 & 0xff);
                    avgg = (pixel >>> 8 & 0xff);
                    avgb = pixel & 0xff;
                    pixel = originalPixels[((dy + py) * sourceWidth) + dx];
                    avgr += (pixel >>> 16 & 0xff);
                    avgg += (pixel >>> 8 & 0xff);
                    avgb += pixel & 0xff;
                    pixel = originalPixels[(dy * sourceWidth) + dx + px];
                    avgr += (pixel >>> 16 & 0xff);
                    avgg += (pixel >>> 8 & 0xff);
                    avgb += pixel >>> 0 & 0xff;
                    pixel = originalPixels[(dy * sourceWidth) + dx - nx];
                    avgr += (pixel >>> 16 & 0xff);
                    avgg += (pixel >>> 8 & 0xff);
                    avgb += pixel >>> 0 & 0xff;
                    resizedPixels[index] = 0xff << 24 | (avgr >> 2) << 16 | (avgg >> 2) << 8 | (avgb >> 2);
                }
                else
                {
                    resizedPixels[index] = pixel;
                }
            }
        }

        //return an immutable image based on the resized pixels
        return Image.createRGBImage(resizedPixels, targetWidth, targetHeight, true);
    }    
}
