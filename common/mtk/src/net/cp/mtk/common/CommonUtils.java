/**
 * Copyright © 2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.mtk.common;


public abstract class CommonUtils
{
    private static final String BASE64_CHARS =   "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    private static final byte[] BASE64_VALUES =
    {
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,     /* 00 - 0F */
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,     /* 10 - 1F */
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63,     /* 20 - 2F */
        52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1,     /* 30 - 3F */
        -1,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14,     /* 40 - 4F */
        15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1,     /* 50 - 5F */
        -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,     /* 60 - 6F */
        41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1,     /* 70 - 7F */
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,     /* 80 - 8F */
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,     /* 90 - 9F */
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,     /* A0 - AF */
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,     /* B0 - BF */
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,     /* C0 - CF */
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,     /* D0 - DF */
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,     /* E0 - EF */
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,     /* F0 - FF */
    };
    

    private CommonUtils()
    {
        super();
    }
    

    /** 
     * Encodes the specified data using base64.
     * 
     * Each encoded base64 character will be one of <tt>A-Z</tt>, <tt>a-z</tt>, <tt>0-9</tt>, '<tt>+</tt>', '<tt>/</tt>' 
     * or '<tt>=</tt>' (Padding).
     * 
     * @param data   the data to be base64 encoded. Must not be null.
     * @param offset the index of the first byte to be encoded.
     * @param length the number of bytes to be encoded.
     * @param output the buffer where each encoded base64 character should be placed. Must not be null.
     */
    public static void base64Encode(byte[] data, int offset, int length, StringBuffer output)
    {
        int inputLen = length;
        int inputIndex = offset;        
        while (inputLen >= 3)
        {
            int b0 = data[inputIndex + 0] & 0xFF;
            int b1 = data[inputIndex + 1] & 0xFF;
            int b2 = data[inputIndex + 2] & 0xFF;
            
            output.append( BASE64_CHARS.charAt( (b0 >> 2) ) );
            output.append( BASE64_CHARS.charAt( ((b0 & 3) << 4) | (b1 >> 4) ) );
            output.append( BASE64_CHARS.charAt( (((b1 & 15) << 2) | (b2 >> 6) ) ) );
            output.append( BASE64_CHARS.charAt( (b2 & 63) ) );
    
            inputLen -= 3;
            inputIndex += 3;
        }
    
        //encode trailing partial group of characters
        if (inputLen == 2)
        {
            int b0 = data[inputIndex + 0] & 0xFF;
            int b1 = data[inputIndex + 1] & 0xFF;
            
            output.append( BASE64_CHARS.charAt( (b0 >> 2) ) );
            output.append( BASE64_CHARS.charAt( ((b0 & 3) << 4) | (b1 >> 4) ) );
            output.append( BASE64_CHARS.charAt( ((b1 & 15) << 2) ) );
            output.append('=');
        }
        else if (inputLen == 1)
        {
            int b0 = data[inputIndex + 0] & 0xFF;
            
            output.append( BASE64_CHARS.charAt( (b0 >> 2) ) );
            output.append( BASE64_CHARS.charAt( ((b0 & 3) << 4) ) );
            output.append("==");
        }
    }

    /** 
     * Returns the base64 encoding of the specified data.
     * 
     * Each encoded base64 character in the returned string will be one of <tt>A-Z</tt>, <tt>a-z</tt>, <tt>0-9</tt>, 
     * '<tt>+</tt>', '<tt>/</tt>' or '<tt>=</tt>' (Padding).
     * 
     * @param data   the data to be base64 encoded. Must not be null.
     * @param offset the index of the first byte to be encoded.
     * @param length the number of bytes to be encoded.
     * @return the base64 encoding of the specified data. Will not be null or empty.
     */
    public static String base64Encode(byte[] data, int offset, int length)
    {
        int outputLen = ((length + 2) / 3) * 4;
        StringBuffer output = new StringBuffer(outputLen);
        base64Encode(data, offset, length, output);
        return output.toString();
    }    

    /** 
     * Returns the base64 encoding of the specified data.
     * 
     * Each encoded base64 character in the returned string will be one of <tt>A-Z</tt>, <tt>a-z</tt>, <tt>0-9</tt>, 
     * '<tt>+</tt>', '<tt>/</tt>' or '<tt>=</tt>' (Padding).
     * 
     * @param data the data to be base64 encoded. Must not be null.
     * @return the base64 encoding of the specified data. Will not be null or empty.
     */
    public static String base64Encode(byte[] data)
    {
        return base64Encode(data, 0, data.length);
    }    

    /** 
     * Returns the base64 encoding of the specified data.
     * 
     * Each encoded base64 character in the returned string will be one of <tt>A-Z</tt>, <tt>a-z</tt>, <tt>0-9</tt>, 
     * '<tt>+</tt>', '<tt>/</tt>' or '<tt>=</tt>' (Padding).
     * 
     * @param segment the array segment to encode. Must not be null.
     * @return the base64 encoding of the specified array segment. Will not be null or empty.
     */
    public static String base64Encode(ArraySegment segment)
    {
        return base64Encode(segment.getArray(), segment.getStartIndex(), segment.getLength());
    }    

    /** 
     * Decodes the specified base64 encoded string.
     * 
     * Each character in the specified string must be one of <tt>A-Z</tt>, <tt>a-z</tt>, <tt>0-9</tt>, '<tt>+</tt>', 
     * '<tt>/</tt>' or '<tt>=</tt>' (Padding).
     * 
     * @param string the base64 encoding of the data to be decoded. Must not be null.
     * @return the decoded data. Will not be null.
     * @throws IllegalArgumentException if the supplied string doesn't contain valid base64 characters.
     */
    public static byte[] base64Decode(String string)
    {
        if ((string.length() % 4) > 0)
            throw new IllegalArgumentException("Invalid number of base64 characters found.");
            
        String input = string;
        int inputLen = input.length();

        byte[] output = new byte[ ((inputLen / 4) * 3) ];
        int nOutputIndex = 0;        
        
        int inputIndex = 0;        
        while (inputIndex < (inputLen - 3))
        {
            if ( (input.charAt(inputIndex + 2) == '=') && (input.charAt(inputIndex + 3) == '=') )
            {
                byte b0 = BASE64_VALUES[ (input.charAt(inputIndex++) & 255) ];
                byte b1 = BASE64_VALUES[ (input.charAt(inputIndex++) & 255) ];
                if ( (b0 == -1) || (b1 == -1) )
                    throw new IllegalArgumentException("Invalid base64 character found");
                    
                output[nOutputIndex++] = (byte)((b0 << 2) | ((b1 >> 4) & 3));
             
                break;
            }
            else if (input.charAt(inputIndex + 3) == '=')
            {
                byte b0 = BASE64_VALUES[ (input.charAt(inputIndex++) & 255) ];
                byte b1 = BASE64_VALUES[ (input.charAt(inputIndex++) & 255) ];
                byte b2 = BASE64_VALUES[ (input.charAt(inputIndex++) & 255) ];
                if ( (b0 == -1) || (b1 == -1) || (b2 == -1) )
                    throw new IllegalArgumentException("Invalid base64 character found");
                
                output[nOutputIndex++] = (byte)((b0 << 2) | ((b1 >> 4) & 3));
                output[nOutputIndex++] = (byte)(((b1 & 15) << 4) | ((b2 >> 2) & 15));
             
                break;
            }
            else
            {
                byte b0 = BASE64_VALUES[ (input.charAt(inputIndex++) & 255) ];
                byte b1 = BASE64_VALUES[ (input.charAt(inputIndex++) & 255) ];
                byte b2 = BASE64_VALUES[ (input.charAt(inputIndex++) & 255) ];
                byte b3 = BASE64_VALUES[ (input.charAt(inputIndex++) & 255) ];
                if ( (b0 == -1) || (b1 == -1) || (b2 == -1) || (b3 == -1) )
                    throw new IllegalArgumentException("Invalid base64 character found");

                output[nOutputIndex++] = (byte)((b0 << 2) | ((b1 >> 4) & 3));
                output[nOutputIndex++] = (byte)(((b1 & 15) << 4) | ((b2 >> 2) & 15));
                output[nOutputIndex++] = (byte)(((b2 & 3) << 6) | b3);
            }
        }

        //check if we have any trailing data and remove it
        if (output.length != nOutputIndex)
        {
            byte[] bTrimedOutput = new byte[nOutputIndex];
            System.arraycopy(output, 0, bTrimedOutput, 0, nOutputIndex);
            return bTrimedOutput;
        }
        
        return output;
    }
    
    /** 
     * Encodes the specified data using hex.
     * 
     * Each encoded hex character will be one of "<code>0123456789ABCDEF</code>". Therefore, each supplied byte will be 
     * encoded as two hex characters.
     * 
     * @param data   the data to be hex encoded. Must not be null.
     * @param offset the index of the first byte to be encoded.
     * @param length the number of bytes to be encoded.
     * @param output the buffer where each encoded hex character should be placed. Must not be null.
     */
    public static void hexEncode(byte[] data, int offset, int length, StringBuffer output)
    {
        for (int i = 0; i < length; i++)
            hexEncode(data[offset + i], output);
    }

    /** 
     * Returns the hex encoding of the specified data.
     * 
     * Each encoded hex character in the returned string will be one of "<code>0123456789ABCDEF</code>". Therefore, each 
     * supplied byte will be encoded as two hex characters.
     * 
     * @param data   the data to be hex encoded. Must not be null.
     * @param offset the index of the first byte to be encoded.
     * @param length the number of bytes to be encoded.
     * @return the hex encoding of the specified data. Will not be null or empty.
     */
    public static String hexEncode(byte[] data, int offset, int length)
    {
        int outputLen = (length * 2);
        StringBuffer output = new StringBuffer(outputLen);
        hexEncode(data, offset, length, output);
        return output.toString();
    }    

    /** 
     * Returns the hex encoding of the specified data.
     * 
     * Each encoded hex character in the returned string will be one of "<code>0123456789ABCDEF</code>". Therefore, each 
     * supplied byte will be encoded as two hex characters.
     * 
     * @param data the data to be hex encoded. Must not be null.
     * @return the hex encoding of the specified data. Will not be null or empty.
     */
    public static String hexEncode(byte[] data)
    {
        return hexEncode(data, 0, data.length);
    }    

    /** 
     * Returns the hex encoding of the specified byte.
     * 
     * Each encoded hex character will be one of "<code>0123456789ABCDEF</code>". Therefore, the supplied byte will be 
     * encoded as two hex characters.
     * 
     * @param b the data to be hex encoded.
     * @param output the buffer where each encoded hex character should be placed. Must not be null.
     */
    public static void hexEncode(byte b, StringBuffer output)
    {
        String outputByte = Integer.toHexString(b & 0xFF).toUpperCase();
        if (outputByte.length() == 1)
            output.append('0');
        output.append(outputByte);
    }    
    
    /** 
     * Decodes the specified hex encoded string.
     * 
     * Each character in the specified string must be one of "<code>0123456789ABCDEF</code>".
     * 
     * @param string the hex encoding of the data to be decoded. Must not be null.
     * @return the decoded data. Will not be null.
     * @throws IllegalArgumentException if the supplied string doesn't contain valid hex characters.
     */
    public static byte[] hexDecode(String string)
    {
        if ((string.length() % 2) > 0)
            throw new IllegalArgumentException("Invalid number of Hex characters found.");
            
        int byteCount = string.length() / 2;
        byte outputBytes[] = new byte[byteCount];
        for (int i = 0; i < byteCount; i++)
        {
            try
            {
                //get the hex (2 character) representation and convert
                String outputByte = string.substring((2 * i), ((2 * i) + 2));
                outputBytes[i] = (byte)(0xFF & Integer.parseInt(outputByte, 16));
            }
            catch (NumberFormatException e)
            {
                throw new IllegalArgumentException("Invalid Hex characters found");
            }
        }
        
        return outputBytes;
    }
    

    /**
     * Returns TRUE if the specified segments of two arrays are the same.
     * 
     * @param array1    the first array to compare. May be null or empty.
     * @param offset1   the index of the first byte in the first array to compare. Must be >=0 and < array1.length. 
     * @param array2    the second array to compare. May be null or empty.
     * @param offset2   the index of the first byte in the second array to compare. Must be >=0 and < array2.length.
     * @param length    the number of bytes to compare. 
     * @param unsigned  indicates whether or not an unsigned compare of each byte should be performed.  
     * @return TRUE if the specified segments of both arrays are the same.
     */
    public static boolean isEquals(byte[] array1, int offset1, byte[] array2, int offset2, int length, boolean unsigned)
    {
        //neither array is present - consider it a match
        if ( (array1 == null) && (array2 == null) )
            return true;

        //one of the arrays is not present - consider it a mismatch
        if ( (array1 == null) || (array2 == null) )
            return false;

        //one or both arrays aren't sufficiently long - consider it a mismatch
        if ( ((offset1 + length) > array1.length) || ((offset2 + length) > array2.length) )
            return false;
        
        if (unsigned)
        {
            //compare each unsigned array element
            for (int i = 0; i < length; i++)
            {
                //element values are different - consider it a mismatch
                if ((array1[offset1 + i] & 0xFF) != (array2[offset2 + i] & 0xFF))
                    return false;
            }
        }
        else
        {
            //compare each array element
            for (int i = 0; i < length; i++)
            {
                //element values are different - consider it a mismatch
                if (array1[offset1 + i] != array2[offset2 + i])
                    return false;
            }
        }
        
        //arrays are a match
        return true;
    }

    /**
     * Returns TRUE if the specified arrays are the same.
     * 
     * @param array1    the first array to compare. May be null or empty.
     * @param array2    the second array to compare. May be null or empty.
     * @param unsigned  indicates whether or not an unsigned compare of each byte should be performed.  
     * @return TRUE if the specified arrays are the same.
     */
    public static boolean isEquals(byte[] array1, byte[] array2, boolean unsigned)
    {
        //neither array is present - consider it a match
        if ( (array1 == null) && (array2 == null) )
            return true;

        //one of the arrays is not present - consider it a mismatch
        if ( (array1 == null) || (array2 == null) )
            return false;

        //array lengths are different - consider it a mismatch
        if (array1.length != array2.length)
            return false;
        
        return isEquals(array1, 0, array2, 0, array1.length, unsigned);
    }

    /**
     * Returns TRUE if the specified signed arrays are the same.
     * 
     * @param array1    the first array to compare. May be null or empty.
     * @param array2    the second array to compare. May be null or empty.
     * @return TRUE if the specified signed arrays are the same.
     */
    public static boolean isEquals(byte[] array1, byte[] array2)
    {
        return isEquals(array1, array2, false);
    }


    /**
     * Returns whether or not the specified value is present in the specified array.
     * 
     * @param array the array to examine. May be null or empty.
     * @param value the value to look for.
     * @return TRUE if the specified value is present in the array, FALSE otherwise.
     */
    public static boolean contains(int[] array, int value)
    {
        if ( (array == null) || (array.length <= 0) )
            return false;
        
        for (int i = 0; i < array.length; i++)
        {
            if (array[i] == value)
                return true;
        }
        
        return false;
    }
    
    /**
     * Compares the specified version numbers.
     * 
     * Both version numbers must be in standard dotted notation and each component of the version number must be a valid 
     * positive integer (or zero). The number of components doesn't have to be the same - for example, the version "2.0" 
     * can be compared to "2.1.3.12" and is equivalent to comparing "2.0.0.0" and "2.1.3.12".  
     * 
     * @param version1 the first version number to compare. Must not be null.
     * @param version2 the second version number to compare. Must not be null.
     * @return 1 if the first version is greater than the second, -1 if the first version is less than the second and 0 if both are the same.
     * @throws IllegalArgumentException if the specified strings are not valid version strings.
     */
    public static int compareVersions(String version1, String version2)
    {
        try
        {
            //split each version string into individual components
            String[] components1 = StringUtils.splitString(version1, '.');
            String[] components2 = StringUtils.splitString(version2, '.');
            
            //compare each component 
            int componentCount = Math.max(components1.length, components2.length);
            for (int i = 0; i < componentCount; i++)
            {
                int component1 = (i < components1.length) ? Integer.parseInt(components1[i]) : 0;
                int component2 = (i < components2.length) ? Integer.parseInt(components2[i]) : 0;
                if ( (component1 < 0) || (component2 < 0) )
                    throw new IllegalArgumentException("Invalid version strings ('" + version1 + "' and '" + version2 + "')");
                
                if (component1 > component2)
                    return 1;
                else if (component1 < component2)
                    return -1;
            }

            return 0;
        }
        catch (Throwable e)
        {
            throw new IllegalArgumentException("Invalid version strings ('" + version1 + "' and '" + version2 + "')");
        }
    }
}
