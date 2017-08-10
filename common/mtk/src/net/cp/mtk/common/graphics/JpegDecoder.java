/**
 * Copyright © 2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.mtk.common.graphics;


import java.io.IOException;
import java.io.InputStream;

import net.cp.mtk.common.ArraySegment;


/**
 * A class implementing a streaming JPEG decoder.
 * 
 * This decoder can decode JPEG data and produce a raw ARGB array of decoded data of the required size. This means that 
 * even large images can be decoded without having to use a lot of memory, as long as the output size isn't too large.
 * 
 * @see JpegDecoder#decode(InputStream, int, int)
 */
public class JpegDecoder
{
    // Comments refer to ITU recommendation T.81
    //
    // This class is mutilated by optimisation:
    // - merged with BitStream
    // - inlined methods
    // - minimised array access by index
    // - made all variables static
    // - replaced float arithmetic with shifted integer arithmetic
    //
    // Making all variables static had a noticeable effect on speed, but strangely enough making 
    // methods static didn't. Turning local variables into instance variables did not increase 
    // performance: what was saved by only declaring the variables once, was lost by the overhead 
    // of accessing instance variables. Removing use of exceptions didn't make the decoder any faster 
    // either.

    // Table B.1
    private static final byte SOF0 = (byte)0xC0;
    private static final byte SOF1 = (byte)0xC1;
    private static final byte SOF2 = (byte)0xC2;
    private static final byte SOF3 = (byte)0xC3;
    private static final byte SOF5 = (byte)0xC5;
    private static final byte SOF6 = (byte)0xC6;
    private static final byte SOF7 = (byte)0xC7;
    private static final byte SOF9 = (byte)0xC9;
    private static final byte SOF10 = (byte)0xCA;
    private static final byte SOF11 = (byte)0xCB;
    private static final byte SOF13 = (byte)0xCD;
    private static final byte SOF14 = (byte)0xCE;
    private static final byte SOF15 = (byte)0xCF;
    private static final byte DHT = (byte)0xC4;
    private static final byte SOI = (byte)0xD8;
    private static final byte EOI = (byte)0xD9;
    private static final byte SOS = (byte)0xDA;
    private static final byte DQT = (byte)0xDB;
    private static final byte DRI = (byte)0xDD;
    private static final byte APP0 = (byte)0xE0;
    private static final byte APP1 = (byte)0xE1;
    private static final byte APP2 = (byte)0xE2;
    private static final byte APP3 = (byte)0xE3;
    private static final byte APP4 = (byte)0xE4;
    private static final byte APP5 = (byte)0xE5;
    private static final byte APP6 = (byte)0xE6;
    private static final byte APP7 = (byte)0xE7;
    private static final byte APP8 = (byte)0xE8;
    private static final byte APP9 = (byte)0xE9;
    private static final byte APP10 = (byte)0xEA;
    private static final byte APP11 = (byte)0xEB;
    private static final byte APP12 = (byte)0xEC;
    private static final byte APP13 = (byte)0xED;
    private static final byte APP14 = (byte)0xEE;
    private static final byte APP15 = (byte)0xEF;
    private static final byte COM = (byte)0xFE;
    
    // Figure A.6
    private static final int[] ZIGZAG_X = { 0, 1, 0, 0, 1, 2, 3, 2, 1, 0, 0, 1, 2, 3, 4, 5, 4, 3, 2, 1, 0, 0, 1, 2, 3,
                                            4, 5, 6, 7, 6, 5, 4, 3, 2, 1, 0, 1, 2, 3, 4, 5, 6, 7, 7, 6, 5, 4, 3, 2, 3,
                                            4, 5, 6, 7, 7, 6, 5, 4, 5, 6, 7, 7, 6, 7 };
    private static final int[] ZIGZAG_Y = { 0, 0, 1, 2, 1, 0, 0, 1, 2, 3, 4, 3, 2, 1, 0, 0, 1, 2, 3, 4, 5, 6, 5, 4, 3,
                                            2, 1, 0, 0, 1, 2, 3, 4, 5, 6, 7, 7, 6, 5, 4, 3, 2, 1, 2, 3, 4, 5, 6, 7, 7,
                                            6, 5, 4, 3, 4, 5, 6, 7, 7, 6, 5, 6, 7, 7 };
    private static final int[] ZIGZAG = new int[64];
    static
    {
        for (int i = 0; i < 64; i++)
        {
            ZIGZAG[i] = ZIGZAG_Y[i] * 8 + ZIGZAG_X[i];
        }
    }

    // precomputed sine/cosine values
    private static final int COS1 = (int)(128 * Math.cos(Math.PI / 16));
    private static final int SIN1 = (int)(128 * Math.sin(Math.PI / 16));
    private static final int COS2 = (int)(128 * Math.cos(2 * Math.PI / 16));
    private static final int SIN2 = (int)(128 * Math.sin(2 * Math.PI / 16));
    private static final int COS3 = (int)(128 * Math.cos(3 * Math.PI / 16));
    private static final int SIN3 = (int)(128 * Math.sin(3 * Math.PI / 16));
    private static final int DIVSQRT2 = (int)(128 / Math.sqrt(2));

    // private BitStream input;
    private static int widthTh;
    private static int heightTh;

    // image decoding details
    private static int scaleX;
    private static int scaleY;
    private static final int NUMBER_OF_COMPONENTS = 3;
    private static final int[] compId = new int[NUMBER_OF_COMPONENTS];
    private static final int[] compHorSampleFactorx8 = new int[NUMBER_OF_COMPONENTS];
    private static final int[] compVerSampleFactorx8 = new int[NUMBER_OF_COMPONENTS];
    private static final int[] compQuantizationTable = new int[NUMBER_OF_COMPONENTS];
    private static final int[] compDCTable = new int[NUMBER_OF_COMPONENTS];
    private static final int[] compACTable = new int[NUMBER_OF_COMPONENTS];
    private static int[][][] compSample = new int[NUMBER_OF_COMPONENTS][][];
    private static final int[] compPred = new int[NUMBER_OF_COMPONENTS];
    private static int numberOfMCUs;
    private static int[][] sample; // one tile worth of samples
    
    // temporary results for a data unit
    private static final int[] zz = new int[64];
    private static final int[] sample1 = new int[64];
    private static final int[] sample2 = new int[64];
    
    // Huffman tables
    private static final int MAX_HUFFMAN_CODES = 512;
    private static final byte[] numberOfValues = new byte[16];
    
    private static final byte[] huffValue = new byte[MAX_HUFFMAN_CODES];

    // current DC table
    private static int[] dcTableStartIndex;
    private static int[] dcTableMinCode;
    private static int[] dcTableMaxCode;
    private static byte[] dcTableValue;

    // DC table 0
    private static final int[] dcTable0StartIndex = new int[16];
    private static final int[] dcTable0MinCode = new int[16];
    private static final int[] dcTable0MaxCode = new int[16];
    private static byte[] dcTable0Value;

    // DC table 1
    private static final int[] dcTable1StartIndex = new int[16];
    private static final int[] dcTable1MinCode = new int[16];
    private static final int[] dcTable1MaxCode = new int[16];
    private static byte[] dcTable1Value;

    // current AC table
    private static int[] acTableStartIndex;
    private static int[] acTableMinCode;
    private static int[] acTableMaxCode;
    private static byte[] acTableValue;

    // AC table 0
    private static final int[] acTable0StartIndex = new int[16];
    private static final int[] acTable0MinCode = new int[16];
    private static final int[] acTable0MaxCode = new int[16];
    private static byte[] acTable0Value;

    // AC table 1
    private static final int[] acTable1StartIndex = new int[16];
    private static final int[] acTable1MinCode = new int[16];
    private static final int[] acTable1MaxCode = new int[16];
    private static byte[] acTable1Value;

    // current Q table
    private static byte[] quantizationTable;
    
    // Q table 0
    private static final byte[] quantizationTable0 = new byte[64];
    
    // Q table 1
    private static final byte[] quantizationTable1 = new byte[64];
    
    // output
    private static int width;
    private static int height;
    private static int tileHeight;
    private static int tileWidth;
    private static boolean thumbnailFound;
    private static int[] output;


    /**
     * Returns the decoded RGB data from the specified JPEG stream.
     * 
     * @param jpegStream    the JPEG input stream to decode. Must not be null.
     * @param maxWidth      the maximum width of the resulting image, or -1 to keep the original width.
     * @param maxHeight     the maximum height of the resulting image, or -1 to keep the original height.
     * @return the decoded RGB data.
     * @throws IOException if the JPEG stream couldn't be decoded.
     */
    public static int[] decode(InputStream jpegStream, int maxWidth, int maxHeight) 
        throws IOException
    {
        //synchronized to ensure it's thread-safe
        synchronized(JpegDecoder.class)
        {
            JpegDecoder jpegDecoder = new JpegDecoder();
            return jpegDecoder.extractStream(jpegStream, maxWidth, maxHeight);
        }
    }
    
    /**
     * Returns the segment containing the thumbnail image in the specified JPEG image data, or null if no thumbnail was found.
     * 
     * @param imageData the JPEG encoded image data to search. May be null or empty.
     * @param offset    the index of the first byte to examine. Must be >=0 and < imageData.length.
     * @param length    the maximum number of bytes to examine, or 0 to examine all data.
     * @return the segment indicating the start and end of the thumbnail image, or null if no thumbnail was found.
     */
    public static ArraySegment findEmbeddedThumbnail(byte[] imageData, int offset, int length)
    {
        //check the parameters
        if ((imageData == null) || (imageData.length <= 4) || (offset < 0) || (offset >= imageData.length) )
            return null;
        
        //adjust the length so it doesn't exceed the data size
        if ( (length <= 0) || (length > (imageData.length - offset)) )
            length = imageData.length - offset;
        int maxIndex = offset + length;
        
        //make sure it's a JPEG we're reading
        if ( ((imageData[offset] & 0xFF) != 0xFF) || ((imageData[offset + 1] & 0xFF) != 0xD8) )
            return null;
        
        //now find the JPEG header of embedded thumbnail
        int index = offset+2;        //skip JPEG header
        int thumbStartIndex = -1;    //-1 means not found
        int thumbEndIndex = -1;      //-1 means not found
        for (; index < maxIndex; index++)
        {
            if ((imageData[index] & 0xFF) == 0xFF && (imageData[index + 1] & 0xFF) == 0xD8)
            {
                thumbStartIndex = index;
                break;
            }
        }

        //no thumbnail found?
        if (thumbStartIndex < 0)
            return null;
        
        //skip thumbnail's JPEG header
        index += 2;
        
        //find end bytes of thumbnail
        for (; index < maxIndex; index++)
        {
            if ((imageData[index] & 0xFF) == 0xFF && (imageData[index + 1] & 0xFF) == 0xD9)
            {
                thumbEndIndex = index + 2;
                break;
            }
        }
        
        //could not find the end of the thumbnail
        if (thumbEndIndex <= 0)
            return null;

        //return the segment details
        return new ArraySegment(imageData, thumbStartIndex, thumbEndIndex);
    }

    
    private JpegDecoder()
    {
        super();
    }
    
    /**
     * Extracts a thumbnail from a stream.
     */
    private int[] extractStream(InputStream in, int maxWidth, int maxHeight) throws IOException
    {
        initializeBitStream(in, 4096);
        output = null;
        widthTh = maxWidth;
        heightTh = maxHeight;
        thumbnailFound = false;
        extract();
        int[] result = output;
        output = null;
        return result;
    }

    /**
     * Extracts a thumbnail from a stream and places it in a integer array.
     */
    private void extract() throws IOException
    {
        // B.2.1
        byte b = decodeMarker();
        if (b != SOI)
        {
            throw new IOException("Missing SOI");
        }
        decodeFrame();
        if (thumbnailFound)
        {
            // stopped decoding
        }
        else
        {
            BitStream_align();
            b = decodeMarker();
            if (b != EOI)
            {
                throw new IOException("Missing EOI");
            }
        }
    }

    /**
     * Initialises the output array.
     */
    private void initOutput()
    {
        if (widthTh <= 0)
            widthTh = width;
        if (heightTh <= 0)
            heightTh = height;

        if ( (width <= widthTh) && (height <= heightTh) )
        {
            //if the image is already smaller than the requested size, just leave it as-is
            widthTh = width;
            heightTh = height;
        }
        else
        {
            //make sure we retain the original aspect ratio
            if ((width - widthTh) > (height - heightTh))
                heightTh = (widthTh * height) / width;
            else
                widthTh = (heightTh * width) / height;
        }
        
        scaleX = width / widthTh;
        scaleY = height / heightTh;

        output = new int[widthTh * heightTh];
    }

    /**
     * Decodes a frame in the JPEG.
     */
    private void decodeFrame() throws IOException
    {
        byte b = 0;
        // B.2.1
        boolean headerFound = false;
        while (!headerFound)
        {
            b = decodeMarker();
            switch (b)
            {
                case DQT:
                    decodeQuantizationTables();
                    break;
                case DHT:
                    decodeHuffmanTables();
                    break;
                case APP0:
                    decodeIdentifier("JFIF");
                    if (thumbnailFound)
                    {
                        return;
                    }
                    break;
                case APP1:
                    decodeIdentifier("Exif");
                    if (thumbnailFound)
                    {
                        return;
                    }
                    break;
                case APP14:
                    decodeIdentifier("Adobe");
                    break;
                case APP2:
                case APP3:
                case APP4:
                case APP5:
                case APP6:
                case APP7:
                case APP8:
                case APP9:
                case APP10:
                case APP11:
                case APP12:
                case APP13:
                case APP15:
                    decodeIdentifier("");
                    break;
                case COM:
                    decodeComment();
                    break;
                case SOF0:
                case SOF1:
                case SOF2:
                case SOF3:
                case SOF5:
                case SOF6:
                case SOF7:
                case SOF9:
                case SOF10:
                case SOF11:
                case SOF13:
                case SOF14:
                case SOF15:
                    headerFound = true;
                    break;
                default:
                    throw new IOException("Unsupported marker " + Integer.toHexString(b));
            }
        }
        if (b != SOF0)
        {
            throw new IOException("Unsupported JPEG encoding");
        }
        decodeFrameHeader();
        // sequential DCT-based: one scan
        decodeScan();
    }

    /**
     * Decodes the header of a frame.
     */
    private void decodeFrameHeader() throws IOException
    {
        // B.2.2
        int frameHeaderLength = BitStream_next2Bytes();
        int precision = BitStream_nextByte();
        if (precision != 8)
        {
            throw new IOException("Unsupported precision");
        }
        height = BitStream_next2Bytes();
        width = BitStream_next2Bytes();
        int numberOfComponents = BitStream_nextByte();
        if (frameHeaderLength != 8 + 3 * numberOfComponents)
        {
            throw new IOException("Invalid frame header length");
        }
        if (numberOfComponents != NUMBER_OF_COMPONENTS)
        {
            throw new IOException("Unsupported number of components.");
        }
        tileWidth = 0;
        tileHeight = 0;
        for (int index = 0; index < NUMBER_OF_COMPONENTS; index++)
        {
            compId[index] = BitStream_nextByte();
            compHorSampleFactorx8[index] = BitStream_next4Bits() * 8;
            if (compHorSampleFactorx8[index] > tileWidth)
            {
                tileWidth = compHorSampleFactorx8[index];
            }
            compVerSampleFactorx8[index] = BitStream_next4Bits() * 8;
            if (compVerSampleFactorx8[index] > tileHeight)
            {
                tileHeight = compVerSampleFactorx8[index];
            }
            compQuantizationTable[index] = BitStream_nextByte();
            compSample[index] = new int[compVerSampleFactorx8[index]][compHorSampleFactorx8[index]];
        }
    }

    /**
     * Decodes a scan in the JPEG.
     */
    private void decodeScan() throws IOException
    {
        numberOfMCUs = 0;
        byte b = 0;
        // B.2.1
        boolean headerFound = false;
        while (!headerFound)
        {
            b = decodeMarker();
            switch (b)
            {
                case DQT:
                    decodeQuantizationTables();
                    break;
                case DHT:
                    decodeHuffmanTables();
                    break;
                case DRI:
                    decodeDRI();
                    break;
                case APP0:
                case APP1:
                case APP2:
                case APP3:
                case APP4:
                case APP5:
                case APP6:
                case APP7:
                case APP8:
                case APP9:
                case APP10:
                case APP11:
                case APP12:
                case APP13:
                case APP14:
                case APP15:
                    decodeIdentifier("");
                    break;
                case COM:
                    decodeComment();
                    break;
                case SOS:
                    headerFound = true;
                    break;
                default:
                    throw new IOException("Unsupported marker " + Integer.toHexString(b));
            }
        }
        decodeScanHeader();
        int h = height % tileHeight;
        if (h != 0)
        {
            height += tileHeight - h;
        }
        int w = width % tileWidth;
        if (w != 0)
        {
            width += tileWidth - w;
        }
        initOutput();
        decodeEntropyCodedSegments();
    }

    /**
     * Decodes the header of a scan.
     */
    private void decodeScanHeader() throws IOException
    {
        // B.2.3
        int scanHeaderLength = BitStream_next2Bytes();
        int numberOfComponents = BitStream_nextByte();
        if (scanHeaderLength != 6 + 2 * numberOfComponents)
        {
            throw new IOException("Invalid scan header length");
        }
        if (numberOfComponents != NUMBER_OF_COMPONENTS)
        {
            throw new IOException("Unsupported number of components");
        }
        for (int index = 0; index < NUMBER_OF_COMPONENTS; index++)
        {
            int id = BitStream_nextByte();
            int j = 0;
            while (compId[j] != id)
            {
                j++;
            }
            compDCTable[j] = BitStream_next4Bits();
            compACTable[j] = BitStream_next4Bits();
            compPred[j] = 0;
        }
        int ss = BitStream_nextByte();
        if (ss != 0)
        {
            throw new IOException("Unsupported start of predictor selection");
        }
        int se = BitStream_nextByte();
        if (se != 63)
        {
            throw new IOException("Unsupported end of predictor selection");
        }
        int a = BitStream_nextByte();
        if (a != 0)
        {
            throw new IOException("Unsupported approximation bit");
        }
    }

    /**
     * Decodes quantisation tables.
     */
    private void decodeQuantizationTables() throws IOException
    {
        // B.2.4.1
        int specLength = BitStream_next2Bytes() - 2;
        while (specLength > 0)
        {
            int precision = BitStream_next4Bits();
            if (precision != 0)
            { 
                // 8-bit
                throw new IOException("Unsupported precision " + precision);
            }
            int dest = BitStream_next4Bits();
            if (dest == 0)
            {
                quantizationTable = quantizationTable0;
            }
            else
            {
                quantizationTable = quantizationTable1;
            }
            for (int i = 0; i < 64; i++)
            {
                quantizationTable[i] = BitStream_nextByte();
            }
            specLength -= 65;
        }
    }

    /**
     * Decodes Huffman tables.
     */
    private void decodeHuffmanTables() throws IOException
    {
        // B.2.4.2
        int specLength = BitStream_next2Bytes() - 2;
        while (specLength > 0)
        {
            int tableClass = BitStream_next4Bits();
            int dest = BitStream_next4Bits();
            if (tableClass == 0)
            {
                if (dest == 0)
                {
                    dcTableStartIndex = dcTable0StartIndex;
                    dcTableMinCode = dcTable0MinCode;
                    dcTableMaxCode = dcTable0MaxCode;
                }
                else
                {
                    dcTableStartIndex = dcTable1StartIndex;
                    dcTableMinCode = dcTable1MinCode;
                    dcTableMaxCode = dcTable1MaxCode;
                }
            }
            else
            {
                if (dest == 0)
                {
                    dcTableStartIndex = acTable0StartIndex;
                    dcTableMinCode = acTable0MinCode;
                    dcTableMaxCode = acTable0MaxCode;
                }
                else
                {
                    dcTableStartIndex = acTable1StartIndex;
                    dcTableMinCode = acTable1MinCode;
                    dcTableMaxCode = acTable1MaxCode;
                }
            }
            for (int i = 0; i < 16; i++)
            {
                numberOfValues[i] = BitStream_nextByte();
            }
            specLength -= 17;
            // C.2
            int size = 0; // number of bits in Huffman code - 1
            int code = 0;
            int len = 0;
            while (size < 16)
            {
                dcTableStartIndex[size] = len; // using dcTable, but could also be an acTable
                dcTableMinCode[size] = code;
                while (numberOfValues[size] > 0)
                {
                    code++;
                    huffValue[len++] = BitStream_nextByte();
                    numberOfValues[size]--;
                }
                dcTableMaxCode[size] = code - 1;
                size++;
                code <<= 1;
            }
            if (tableClass == 0)
            {
                if (dest == 0)
                {
                    dcTable0Value = new byte[len];
                    System.arraycopy(huffValue, 0, dcTable0Value, 0, len);
                }
                else
                {
                    dcTable1Value = new byte[len];
                    System.arraycopy(huffValue, 0, dcTable1Value, 0, len);
                }
            }
            else
            {
                if (dest == 0)
                {
                    acTable0Value = new byte[len];
                    System.arraycopy(huffValue, 0, acTable0Value, 0, len);
                }
                else
                {
                    acTable1Value = new byte[len];
                    System.arraycopy(huffValue, 0, acTable1Value, 0, len);
                }
            }
            specLength -= len;
        }
    }

    /**
     * Decodes a restart interval definition.
     */
    private void decodeDRI() throws IOException
    {
        // B.2.4.4
        int dataLength = BitStream_next2Bytes();
        if (dataLength != 4)
        {
            throw new IOException("Invalid restart interval definition length.");
        }
        numberOfMCUs = BitStream_next2Bytes();
    }

    /**
     * Decodes (skips) a comment.
     */
    private void decodeComment() throws IOException
    {
        // B.2.4.5
        int dataLength = BitStream_next2Bytes() - 2;
        while (dataLength > 0)
        {
            BitStream_nextByte();
            dataLength--;
        }
    }

    /**
     * Decodes an application specific data segment starting with a given identifier. If the identifier is not present,
     * skips the segment.
     */
    private void decodeIdentifier(String identifier) throws IOException
    {
        // B.2.4.6
        int dataLength = BitStream_next2Bytes() - 2;
        byte[] b = new byte[identifier.length()];
        if (dataLength > b.length)
        {
            for (int i = 0; i < b.length; i++)
            {
                b[i] = BitStream_nextByte();
            }
            dataLength -= b.length;
        }
        if (new String(b).equals(identifier))
        {
            if ("JFIF".equals(identifier))
            {
                dataLength -= decodeJFIF();
            }
            else if ("Exif".equals(identifier))
            {
                dataLength -= decodeExif();
            }
            else if ("Adobe".equals(identifier))
            {
                dataLength -= decodeAdobe();
            }
        }
        if (!thumbnailFound)
        {
            while (dataLength > 0)
            {
                BitStream_nextByte();
                dataLength--;
            }
        }
    }

    /**
     * Decodes a JFIF segment.
     * 
     * @returns Number of bytes processed.
     */
    private int decodeJFIF() throws IOException
    {
        BitStream_nextByte(); // 00
        int version = BitStream_next2Bytes();
        if ((version != 0x11) && (version != 0x12))
        {
            return 3;
        }
        BitStream_nextByte(); // units
        BitStream_next2Bytes(); // x density
        BitStream_next2Bytes(); // y density
        width = BitStream_nextByte();
        height = BitStream_nextByte();
        if ((width > 0) && (height > 0))
        {
            initOutput();
            for (int i = 0; i < output.length; i++)
            {
                byte r = BitStream_nextByte();
                byte g = BitStream_nextByte();
                byte b = BitStream_nextByte();
                output[i] = 0xFF000000 | (r << 16) | (g << 8) | (b);
            }
            thumbnailFound = true;
        }
        return 10 + width * height * 3;
    }

    /**
     * Decodes an Exif segment.
     */
    private int decodeExif() throws IOException
    {
        long fileOffset = BitStream_getBytesRead();
        BitStream_nextByte(); // 00
        BitStream_nextByte(); // 00
        long next = fileOffset + decodeTiffHeader() - BitStream_getBytesRead();
        if (next >= 0)
        {
            // move to first image file directory
            while (next > 0)
            {
                BitStream_nextByte();
                next--;
            }
            next = fileOffset + decodeIFD() - BitStream_getBytesRead();
            if (next >= 0)
            {
                // move to next image file directory
                while (next > 0)
                {
                    BitStream_nextByte();
                    next--;
                }
                next = fileOffset + decodeIFD() - BitStream_getBytesRead();
                if (next >= 0)
                {
                    // move to JPEG data
                    while (next > 0)
                    {
                        BitStream_nextByte();
                        next--;
                    }
                    BitStream_setByteOrder(BitStream_BIG_ENDIAN);
                    extract();
                    thumbnailFound = true;
                }
            }
        }
        BitStream_setByteOrder(BitStream_BIG_ENDIAN);
        return (int)(BitStream_getBytesRead() - fileOffset);
    }

    /**
     * Decodes a TIFF header.
     * 
     * @return offset within the Exif segment where the next IFD starts.
     */
    private int decodeTiffHeader() throws IOException
    {
        int byteOrder = BitStream_next2Bytes();
        if ((byteOrder != BitStream_LITTLE_ENDIAN) && (byteOrder != BitStream_BIG_ENDIAN))
        {
            throw new IOException("Invalid byte order");
        }
        BitStream_setByteOrder(byteOrder);
        int fortyTwo = BitStream_next2Bytes();
        if (fortyTwo != 42)
        {
            throw new IOException("Invalid identifier in TIFF header");
        }
        return BitStream_next4Bytes();
    }

    /**
     * Decides an Image File Directory.
     * 
     * @return offset within the Exif segment where the next IFD starts, or if a thumbnail tag is present, where the
     *         thumbnail data starts.
     */
    private int decodeIFD() throws IOException
    {
        int numberOfFields = BitStream_next2Bytes();
        int tag;
        for (int i = 0; (i < numberOfFields); i++)
        {
            tag = BitStream_next2Bytes();
            BitStream_next2Bytes(); // type
            BitStream_next4Bytes(); // count
            if (tag == 0x0201)
            {
                return BitStream_next4Bytes();
            }

            BitStream_nextByte();
            BitStream_nextByte();
            BitStream_nextByte();
            BitStream_nextByte();
        }
        return BitStream_next4Bytes();
    }

    /**
     * Decodes an Adobe segment.
     * 
     * @return int
     */
    private int decodeAdobe()
    {
        // this segment might give information about the color definitions used
        return 0;
    }

    /**
     * Decodes the entropy coded segments.
     */
    private void decodeEntropyCodedSegments() throws IOException
    {
        BitStream_setIgnoreStuffedZeros(true);
        int n = numberOfMCUs;
        if (n == 0)
        {
            n = -1; // no restart intervals
        }
        int numberOfRows = height / tileHeight;
        int numberOfCols = width / tileWidth;
        
        // A.2.3 (interleaved)
        for (int row = 0; row < numberOfRows; row++)
        {
            for (int col = 0; col < numberOfCols; col++)
            {
                if (n == 0)
                {
                    decodeRestartMarker();
                    n = numberOfMCUs;
                }
                decodeMCU(row, col);
                n--;
                compileImage(row, col);
            }
        }
        BitStream_setIgnoreStuffedZeros(false);
    }

    /**
     * Decodes a restart marker: resets predictions for all components.
     */
    private void decodeRestartMarker() throws IOException
    {
        BitStream_align();
        byte b = decodeMarker();
        if ((b & 0xD8) != 0xD0)
        {
            throw new IOException("Missing RST");
        }
        for (int index = 0; index < NUMBER_OF_COMPONENTS; index++)
        {
            compPred[index] = 0;
        }
    }

    /**
     * Decodes the minimal coded unit that contains data for tile and places the result in
     * compSample[0..NUMBER_OF_COMPONENTS].
     */
    private void decodeMCU(int row, int col) throws IOException
    {
        // interleaved: the MCU contains data units for each component
        for (int index = 0; index < NUMBER_OF_COMPONENTS; index++)
        {
            // set up the tables for component [index]
            if (compDCTable[index] == 0)
            {
                dcTableStartIndex = dcTable0StartIndex;
                dcTableMinCode = dcTable0MinCode;
                dcTableMaxCode = dcTable0MaxCode;
                dcTableValue = dcTable0Value;
            }
            else
            {
                dcTableStartIndex = dcTable1StartIndex;
                dcTableMinCode = dcTable1MinCode;
                dcTableMaxCode = dcTable1MaxCode;
                dcTableValue = dcTable1Value;
            }
            if (compACTable[index] == 0)
            {
                acTableStartIndex = acTable0StartIndex;
                acTableMinCode = acTable0MinCode;
                acTableMaxCode = acTable0MaxCode;
                acTableValue = acTable0Value;
            }
            else
            {
                acTableStartIndex = acTable1StartIndex;
                acTableMinCode = acTable1MinCode;
                acTableMaxCode = acTable1MaxCode;
                acTableValue = acTable1Value;
            }
            if (compQuantizationTable[index] == 0)
            {
                quantizationTable = quantizationTable0;
            }
            else
            {
                quantizationTable = quantizationTable1;
            }
            zz[0] = compPred[index];
            sample = compSample[index];
            for (int y = 0; y < compVerSampleFactorx8[index]; y += 8)
            {
                for (int x = 0; x < compHorSampleFactorx8[index]; x += 8)
                {
                    decodeDataUnit(y, x);
                }
            }
            compPred[index] = zz[0];
        }
    }

    /**
     * Decodes a 8x8 pixel data unit. The left top corner of the data unit is at the given coordinates.
     */
    private void decodeDataUnit(int top, int left) throws IOException
    {
        // F.2.1.2
        // decodeDC();
        // private void decodeDC(int dcTable)
        {
            // F.2.2.1
            int len = decodeCodeDC();
            zz[0] += receiveAndExtend(len);
        }
        // decodeACs();
        // private void decodeACs(int acTable)
        {
            for (int i = 1; i < zz.length; i++)
            {
                zz[i] = 0;
            }
            int rs, r, s;
            // F.2.2.2
            for (int i = 1; i < 64; i++)
            {
                rs = decodeCodeAC();
                r = (rs & 0xF0) >> 4;
                s = rs & 0xF;
                if (s == 0)
                {
                    if (r < 0xF)
                    {
                        break;
                    }
                    i += 15;
                }
                else
                {
                    i += r;
                    zz[i] = receiveAndExtend(s);
                }
            }
        }
        int b0, b1, b2, b3, b4, b5, b6, b7;
        int c0, c1, c2, c3, c4, c5, c6, c7;
        {
            // F.2.1.4
            for (int i = 0; i < zz.length; i++)
            {
                sample1[ZIGZAG[i]] = zz[i] * quantizationTable[i];
            }
        }
        {
            // 1-D IDCT on rows (S x K)
            for (int y = 0; y < 64; y += 8)
            {
                b0 = sample1[y + 0] * DIVSQRT2;
                b1 = sample1[y + 4] * DIVSQRT2;
                b2 = sample1[y + 2] << 7;
                b3 = sample1[y + 6] << 7;
                b4 = (sample1[y + 1] - sample1[y + 7]) * DIVSQRT2;
                b6 = sample1[y + 5] << 7;
                b5 = (sample1[y + 1] + sample1[y + 7]) * DIVSQRT2;
                b7 = sample1[y + 3] << 7;
                c0 = (b0 + b1) << 7;
                c1 = (b0 - b1) << 7;
                c2 = (b2 * SIN2 - b3 * COS2);
                c3 = (b2 * COS2 + b3 * SIN2);
                c4 = (b4 + b6) << 7;
                c6 = (b4 - b6) << 7;
                c5 = (b5 - b7) << 7;
                c7 = (b5 + b7) << 7;
                b0 = (c0 + c3) << 7;
                b1 = (c1 + c2) << 7;
                b2 = (c1 - c2) << 7;
                b3 = (c0 - c3) << 7;
                b4 = (c4 * COS3 - c7 * SIN3);
                b5 = (c5 * COS1 - c6 * SIN1);
                b6 = (c5 * SIN1 + c6 * COS1);
                b7 = (c4 * SIN3 + c7 * COS3);
                sample2[y + 0] = (b0 + b7) >> 22;
                sample2[y + 1] = (b1 + b6) >> 22;
                sample2[y + 2] = (b2 + b5) >> 22;
                sample2[y + 3] = (b3 + b4) >> 22;
                sample2[y + 4] = (b3 - b4) >> 22;
                sample2[y + 5] = (b2 - b5) >> 22;
                sample2[y + 6] = (b1 - b6) >> 22;
                sample2[y + 7] = (b0 - b7) >> 22;
            }
            // 1-D IDCT on columns
            for (int x = 0; x < 8; x++)
            {
                b0 = sample2[0 * 8 + x] * DIVSQRT2;
                b1 = sample2[4 * 8 + x] * DIVSQRT2;
                b2 = sample2[2 * 8 + x] << 7;
                b3 = sample2[6 * 8 + x] << 7;
                b4 = (sample2[1 * 8 + x] - sample2[7 * 8 + x]) * DIVSQRT2;
                b6 = sample2[5 * 8 + x] << 7;
                b5 = (sample2[1 * 8 + x] + sample2[7 * 8 + x]) * DIVSQRT2;
                b7 = sample2[3 * 8 + x] << 7;
                c0 = (b0 + b1) << 7;
                c1 = (b0 - b1) << 7;
                c2 = (b2 * SIN2 - b3 * COS2);
                c3 = (b2 * COS2 + b3 * SIN2);
                c4 = (b4 + b6) << 7;
                c6 = (b4 - b6) << 7;
                c5 = (b5 - b7) << 7;
                c7 = (b5 + b7) << 7;
                b0 = (c0 + c3) << 7;
                b1 = (c1 + c2) << 7;
                b2 = (c1 - c2) << 7;
                b3 = (c0 - c3) << 7;
                b4 = (c4 * COS3 - c7 * SIN3);
                b5 = (c5 * COS1 - c6 * SIN1);
                b6 = (c5 * SIN1 + c6 * COS1);
                b7 = (c4 * SIN3 + c7 * COS3);
                int x1 = left + x;
                sample[top + 0][x1] = (b0 + b7) >> 22;
                sample[top + 1][x1] = (b1 + b6) >> 22;
                sample[top + 2][x1] = (b2 + b5) >> 22;
                sample[top + 3][x1] = (b3 + b4) >> 22;
                sample[top + 4][x1] = (b3 - b4) >> 22;
                sample[top + 5][x1] = (b2 - b5) >> 22;
                sample[top + 6][x1] = (b1 - b6) >> 22;
                sample[top + 7][x1] = (b0 - b7) >> 22;
            }
        }
    }

    /**
     * Finds the value for the next Huffman DC code in the input.
     * 
     * @return The value.
     */
    private int decodeCodeDC() throws IOException
    {
        // F.2.2.3
        BitStream_currentBit--;
        int code = (BitStream_buffer[BitStream_currentByte] >> BitStream_currentBit) & 0x1;
        if (BitStream_currentBit == 0)
        {
            BitStream_readByte();
        }
        int size = 0;
        while (code > dcTableMaxCode[size])
        {
            BitStream_currentBit--;
            code = (code << 1) | ((BitStream_buffer[BitStream_currentByte] >> BitStream_currentBit) & 0x1);
            if (BitStream_currentBit == 0)
            {
                BitStream_readByte();
            }
            size++;
        }
        return dcTableValue[dcTableStartIndex[size] + code - dcTableMinCode[size]];
    }

    /**
     * Finds the value for the next Huffman AC code in the input.
     * 
     * @return The value.
     */
    private int decodeCodeAC() throws IOException
    {
        // F.2.2.3
        BitStream_currentBit--;
        int code = (BitStream_buffer[BitStream_currentByte] >> BitStream_currentBit) & 0x1;
        if (BitStream_currentBit == 0)
        {
            BitStream_readByte();
        }
        int size = 0;
        while (code > acTableMaxCode[size])
        {
            BitStream_currentBit--;
            code = (code << 1) | ((BitStream_buffer[BitStream_currentByte] >> BitStream_currentBit) & 0x1);
            if (BitStream_currentBit == 0)
            {
                BitStream_readByte();
            }
            size++;
        }
        return acTableValue[acTableStartIndex[size] + code - acTableMinCode[size]];
    }

    /**
     * Reads in the next coefficient with a given length.
     * 
     * @return The coefficient
     */
    private int receiveAndExtend(int len) throws IOException
    {
        int x = 0;
        // F.2.2.1 and F.2.2.4
        if (len == 0)
        {
            x = 0;
        }
        else
        {
            BitStream_currentBit--;
            int sb = (BitStream_buffer[BitStream_currentByte] >> BitStream_currentBit) & 0x1;
            if (BitStream_currentBit == 0)
            {
                BitStream_readByte();
            }
            x = (sb == 0) ? -1 << 1 : 1;
            while (len-- > 1)
            {
                BitStream_currentBit--;
                x = (x << 1) | ((BitStream_buffer[BitStream_currentByte] >> BitStream_currentBit) & 0x1);
                if (BitStream_currentBit == 0)
                {
                    BitStream_readByte();
                }
            }
            if (sb == 0)
            {
                x++;
            }
        }
        return x;
    }

    /**
     * Compiles the tile from compSample[0..2].
     */
    private void compileImage(int row, int col)
    {
        int yy, cb, cr;
        int r, g, b;
        for (int y = 0, y1 = row * tileHeight; y < tileHeight; y++, y1++)
        {
            if (y1 % scaleY == 0)
            {
                int y2 = y1 / scaleY;
                if (y2 < heightTh)
                {
                    for (int x = 0, x1 = col * tileWidth; x < tileWidth; x++, x1++)
                    {
                        if (x1 % scaleX == 0)
                        {
                            int x2 = x1 / scaleX;
                            if (x2 < widthTh)
                            {
                                yy = (compSample[0][y * compVerSampleFactorx8[0] / tileHeight][x
                                    * compHorSampleFactorx8[0] / tileWidth] + 112) * 298;
                                cb = compSample[1][y * compVerSampleFactorx8[1] / tileHeight][x
                                    * compHorSampleFactorx8[1] / tileWidth];
                                cr = compSample[2][y * compVerSampleFactorx8[2] / tileHeight][x
                                    * compHorSampleFactorx8[2] / tileWidth];
                                r = (yy + 408 * cr) >> 8;
                                if (r < 0)
                                {
                                    r = 0;
                                }
                                else if (r > 255)
                                {
                                    r = 255;
                                }
                                g = (yy - 100 * cb - 208 * cr) >> 8;
                                if (g < 0)
                                {
                                    g = 0;
                                }
                                else if (g > 255)
                                {
                                    g = 255;
                                }
                                b = (yy + 516 * cb) >> 8;
                                if (b < 0)
                                {
                                    b = 0;
                                }
                                else if (b > 255)
                                {
                                    b = 255;
                                }
                                output[y2 * widthTh + x2] = 0xFF000000 | (r << 16) | (g << 8) | (b);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Decodes a marker, that is FF followed by an identifier.
     * 
     * @return The identifier, or 0 if no marker is found.
     */
    private byte decodeMarker() throws IOException
    {
        byte m = 0;
        if (BitStream_isNextByteMarker())
        {
            BitStream_nextByte();
            m = BitStream_nextByte();
        }
        return m;
    }

    /**
     * A buffered stream that lets the client read separate bits. (merged with BaseJpegThumbnailReader for performance
     * reasons).
     */
    private static final byte BitStream_MARKER = (byte)0xFF;
    private static InputStream BitStream_input;
    private static byte[] BitStream_buffer;
    private static int BitStream_bufferLength;
    private static int BitStream_currentByte;
    private static int BitStream_currentBit;
    private static final int BitStream_LITTLE_ENDIAN = 0x4949;
    private static final int BitStream_BIG_ENDIAN = 0x4D4D;
    private static int BitStream_byteOrder;
    private static boolean BitStream_ignoreStuffedZeros;
    private static boolean BitStream_marker;
    private static int BitStream_totalRead;

    /**
     * Wraps an input stream for bit access and buffering.
     */
    private void initializeBitStream(InputStream in, int blockSize) throws IOException
    {
        BitStream_input = in;
        BitStream_buffer = new byte[blockSize];
        BitStream_bufferLength = 0;
        BitStream_byteOrder = BitStream_BIG_ENDIAN;
        BitStream_ignoreStuffedZeros = false;
        BitStream_currentByte = -1;
        BitStream_readByte();
    }

    /*
     * Sets the order in which bytes are interpreted. Can be changed half-way through reading the stream, for example in
     * an Exif marker segment.
     * @param byteOrder BIG_ENDIAN or LITTLE_ENDIAN
     */
    private void BitStream_setByteOrder(int byteOrder)
    {
        BitStream_byteOrder = byteOrder;
    }

    /**
     * Indicates if stuffed zeros (eight 0s directly after eight 1s) have to be skipped. Default is false.
     */
    private void BitStream_setIgnoreStuffedZeros(boolean ignore)
    {
        BitStream_ignoreStuffedZeros = ignore;
    }

    /**
     * Reads the next four bits from the stream.
     * 
     * @return An integer with the next four bits in bit 0-3.
     */
    private int BitStream_next4Bits() throws IOException
    {
        BitStream_currentBit -= 4;
        int b = (BitStream_buffer[BitStream_currentByte] >> BitStream_currentBit) & 0xF;
        if (BitStream_currentBit == 0)
        {
            BitStream_readByte();
        }
        return b;
    }

    /**
     * Checks if the next byte is a marker (eight 1s).
     * 
     * @return True if this is the case.
     */
    private boolean BitStream_isNextByteMarker()
    {
        return BitStream_marker;
    }

    /**
     * Reads the next byte from the stream.
     * 
     * @return The next byte.
     */
    private byte BitStream_nextByte() throws IOException
    {
        byte b = BitStream_buffer[BitStream_currentByte];
        BitStream_readByte();
        return b;
    }

    /**
     * Reads the next two bytes from the stream.
     * 
     * @return An integer with the next two bytes in bit 0-16.
     */
    private int BitStream_next2Bytes() throws IOException
    {
        int n = 0;
        if (BitStream_byteOrder == BitStream_BIG_ENDIAN)
        {
            n = BitStream_buffer[BitStream_currentByte] << 8;
            BitStream_readByte();
            n |= BitStream_buffer[BitStream_currentByte] & 0xFF;
            BitStream_readByte();
        }
        else
        {
            n = BitStream_buffer[BitStream_currentByte] & 0xFF;
            BitStream_readByte();
            n |= (BitStream_buffer[BitStream_currentByte] << 8);
            BitStream_readByte();
        }
        return n;
    }

    /**
     * Reads the next four bytes from the stream.
     * 
     * @return An integer with the next four bytes in bit 0-32.
     */
    private int BitStream_next4Bytes() throws IOException
    {
        int n = 0;
        if (BitStream_byteOrder == BitStream_BIG_ENDIAN)
        {
            n = BitStream_buffer[BitStream_currentByte] << 24;
            BitStream_readByte();
            n |= (BitStream_buffer[BitStream_currentByte] & 0xFF) << 16;
            BitStream_readByte();
            n |= (BitStream_buffer[BitStream_currentByte] & 0xFF) << 8;
            BitStream_readByte();
            n |= BitStream_buffer[BitStream_currentByte] & 0xFF;
            BitStream_readByte();
        }
        else
        {
            n = BitStream_buffer[BitStream_currentByte] & 0xFF;
            BitStream_readByte();
            n |= (BitStream_buffer[BitStream_currentByte] & 0xFF) << 8;
            BitStream_readByte();
            n |= (BitStream_buffer[BitStream_currentByte] & 0xFF) << 16;
            BitStream_readByte();
            n |= BitStream_buffer[BitStream_currentByte] << 24;
            BitStream_readByte();
        }
        return n;
    }

    /**
     * Aligns the stream to the next byte if necessary.
     */
    private void BitStream_align() throws IOException
    {
        if (BitStream_currentBit != 8)
        {
            BitStream_readByte();
        }
    }

    /**
     * Gets the number of complete bytes read.
     */
    private long BitStream_getBytesRead()
    {
        return BitStream_totalRead + BitStream_currentByte;
    }

    private void BitStream_readByte() throws IOException
    {
        BitStream_currentByte++;
        if (BitStream_currentByte == BitStream_bufferLength)
        {
            BitStream_readBlock();
        }
        if (BitStream_marker && (BitStream_buffer[BitStream_currentByte] == 0) && BitStream_ignoreStuffedZeros)
        {
            BitStream_currentByte++;
            if (BitStream_currentByte == BitStream_bufferLength)
            {
                BitStream_readBlock();
            }
        }
        BitStream_marker = (BitStream_buffer[BitStream_currentByte] == BitStream_MARKER);
        BitStream_currentBit = 8;
    }

    private void BitStream_readBlock() throws IOException
    {
        BitStream_totalRead += BitStream_bufferLength;
        if (BitStream_input == null)
        {
            BitStream_bufferLength = BitStream_buffer.length;
        }
        else
        {
            BitStream_bufferLength = BitStream_input.read(BitStream_buffer);
        }
        BitStream_currentByte = 0;
    }
}
