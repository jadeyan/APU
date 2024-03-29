/**
 * Copyright � 2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.mtk.common.io;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Hashtable;


/**
 * Utility class containing some useful methods for handling input/output streams.
 * 
 * @author Denis Evoy
 */
public abstract class StreamUtils
{
    private StreamUtils()
    {
        super();
    }
    

    /**
     * Closes the specified stream, ignoring any exceptions that occur.
     * 
     * @param stream the stream to close.
     */
    public static void closeStream(InputStream stream)
    {
        if (stream == null)
            return;

        try
        {
            stream.close();
        }
        catch (Throwable e)
        {
            //ignore
        }
    }
    
    /**
     * Closes the specified stream, ignoring any exceptions that occur.
     * 
     * @param stream the stream to close.
     */
    public static void closeStream(OutputStream stream)
    {
        if (stream == null)
            return;

        try
        {
            stream.close();
        }
        catch (Throwable e)
        {
            //ignore
        }
    }
    
    /**
     * Closes the specified reader, ignoring any exceptions that occur.
     * 
     * @param reader the reader to close.
     */
    public static void closeReader(Reader reader)
    {
        if (reader == null)
            return;

        try
        {
            reader.close();
        }
        catch (Throwable e)
        {
            //ignore
        }
    }
    
    /**
     * Closes the specified writer, ignoring any exceptions that occur.
     * 
     * @param writer the writer to close.
     */
    public static void closeWriter(Writer writer)
    {
        if (writer == null)
            return;

        try
        {
            writer.close();
        }
        catch (Throwable e)
        {
            //ignore
        }
    }
    

    /**
     * Read the next line from the specified reader.
     * 
     * @param reader         the reader to read from. Must not be null.
     * @param trim           TRUE if the line should be trimmed before being returned. 
     * @param skipEmptyLines TRUE if empty lines (after being trimmed) should be ignored.   
     * @param lineBuffer     the buffer where the line (without the trailing CRLF characters) should be placed. Must not be null.
     * @return the number of characters read, or -1 if there is no more data to read.
     * @throws IOException if the reader couldn't be read.
     */
    public static int readNextLine(Reader reader, boolean trim, boolean skipEmptyLines, StringBuffer lineBuffer)
        throws IOException
    {
        int initialLength = lineBuffer.length();

        int charRead = -1;
        while ((charRead = reader.read()) != -1)
        {
            //trim the start of the line (if required)
            if (trim)
            {
                while ( (charRead != -1) && (charRead <= 0x20) )
                    charRead = reader.read();
            }

            //read everything until CR or LF
            while ( (charRead != -1) && (charRead != 0x0A) && (charRead != 0x0D) )
            {
                lineBuffer.append((char)charRead);
                charRead = reader.read();
            }

            //trim the end of the line (if required)
            int newLength = lineBuffer.length();
            if (trim)
            {
                int trimmedLength = newLength;
                for (int i = (newLength - 1); i >= initialLength; i--)
                {
                    char c = lineBuffer.charAt(i);
                    if (c <= 0x20)
                        trimmedLength--;
                    else
                        break;
                }
                
                if (trimmedLength < newLength)
                {
                    lineBuffer.setLength(trimmedLength);
                    newLength = trimmedLength;
                }
            }

            //check if the line was empty
            int charsRead = newLength - initialLength;
            if ( (skipEmptyLines) && (charsRead <= 0) )
                continue;

            return charsRead;
        }
        
        return -1;
    }

    /**
     * Read the next line from the specified reader.
     * 
     * @param reader         the reader to read from. Must not be null.
     * @param trim           TRUE if the line should be trimmed before being returned. 
     * @param skipEmptyLines TRUE if empty lines (after being trimmed) should be ignored.   
     * @return the next line, or null if there is no more data to read.
     * @throws IOException if the reader couldn't be read.
     */
    public static String readNextLine(Reader reader, boolean trim, boolean skipEmptyLines)
        throws IOException
    {
        StringBuffer lineBuffer = new StringBuffer(256);
        int charsRead = readNextLine(reader, trim, skipEmptyLines, lineBuffer);
        if (charsRead < 0)
            return null;

        return lineBuffer.toString();
    }


    /**
     * Reads the properties from the specified reader.
     * 
     * Each property is contained on a single line and must be in the form:
     * <pre>
     *      &lt;Name&gt;&lt;Separator&gt[Value]
     * </pre>
     * For example:
     * <pre>
     *      Hostname=foo.acme.com
     *      Port=80
     *      Username=
     * </pre>
     * 
     * Each property must have a name and an optional value. The value may be empty and can contain any number of 
     * whitespace characters. If a property appears multiple times, the last value will override all previous values. 
     * Empty lines or comment lines (i.e. lines whose first non-whitespace character is '#') are ignored.
     * 
     * Also note that property names are case-sensitive (so property "Foo" is not the same as property "foo"). 
     * 
     * @param reader    the reader to read from. Must not be null.
     * @param separator the string used to separate the property name and value (e.g. "="). Must not be null or empty.
     * @return a collection of properties keys (String) and their associated values (String). Will not be null.
     * @throws IOException if the reader couldn't be read or a badly formatted property was found.
     */
    public static Hashtable readProperties(Reader reader, String separator)
        throws IOException
    {
        Hashtable properties = new Hashtable();

        int sepLength = separator.length();
        StringBuffer propertyLineBuffer = new StringBuffer(256);
        while (readNextLine(reader, false, true, propertyLineBuffer) >= 0)
        {
            String propertyLine = propertyLineBuffer.toString();
            propertyLineBuffer.setLength(0);

            //ignore empty or commented lines
            String trimmedLine = propertyLine.trim();
            if ( (trimmedLine.length() <= 0) || (trimmedLine.startsWith("#")) )
                continue;
            
            //split into property name and value
            int index = propertyLine.indexOf(separator);
            if (index <= 0)
                throw new IOException("Invalid property '" + propertyLine + "' found");
            
            String value = "";
            String name = propertyLine.substring(0, index);
            if ((index + sepLength) < propertyLine.length())
                value = propertyLine.substring(index + sepLength);
            
            properties.put(name, value);
        }
        
        return properties;
    }

    /**
     * Reads the properties from the specified input stream.
     * 
     * @param stream    the input stream to read from. Must not be null.
     * @param charset   the character set of the data being read (e.g. "UTF-8"). May be null (implies the platform default character set).
     * @param separator the string used to separate the property name and value (e.g. "="). Must not be null or empty.
     * @return a collection of properties keys (String) and their associated values (String). Will not be null.
     * @throws IOException if the input stream couldn't be read or a badly formatted property was found.
     * 
     * @see #readProperties(Reader reader, String separator)
     */
    public static Hashtable readProperties(InputStream stream, String charset, String separator)
        throws IOException
    {
        Reader reader = null;
        try
        {
            if ( (charset == null) || (charset.length() <= 0) )
                reader = new InputStreamReader(stream);
            else
                reader = new InputStreamReader(stream, charset);

            return readProperties(reader, separator);
        }
        finally
        {
            closeReader(reader);
        }
    }
    
    /**
     * Writes the specified properties to the specified writer.
     * 
     * Each property is written to a single line and will be in the form:
     * <pre>
     *      &lt;Name&gt;&lt;Separator&gt[Value]
     * </pre>
     * For example:
     * <pre>
     *      Hostname=foo.acme.com
     *      Port=80
     *      Username=
     * </pre>
     * 
     * @param properties    the properties to write. Must not be null.
     * @param writer        the writer to write to. Must not be null.
     * @param separator     the string used to separate the property name and value (e.g. "="). Must not be null or empty.
     * @throws IOException if the properties couldn't be written.
     */
    public static void writeProperties(Hashtable properties, Writer writer, String separator)
        throws IOException
    {
        for (Enumeration keysEnum = properties.keys(); keysEnum.hasMoreElements(); )
        {
            //ignore empty keys
            String key = (String)keysEnum.nextElement();
            if ( (key == null) || (key.length() <= 0) )
                continue;
            
            //get the value
            String value = (String)properties.get(key);
            if (value == null)
                value = "";

            //write the property as: <Name><Separator><Value>
            writer.write(key);
            writer.write(separator);
            writer.write(value);
            writer.write("\n");
        }
    }

    /**
     * Writes the specified properties to the specified stream.
     * 
     * @param properties    the properties to write. Must not be null.
     * @param stream        the stream to write to. Must not be null.
     * @param charset       the character set to use when writing the data (e.g. "UTF-8"). May be null (implies the platform default character set).
     * @param separator     the string used to separate the property name and value (e.g. "="). Must not be null or empty.
     * @throws IOException if the properties couldn't be written.
     * 
     * @see #writeProperties(Hashtable properties, Writer writer, String separator)
     */
    public static void writeProperties(Hashtable properties, OutputStream stream, String charset, String separator)
        throws IOException
    {
        if (properties.size() <= 0)
            return;

        Writer writer = null;
        try
        {
            if ( (charset == null) || (charset.length() <= 0) )
                writer = new OutputStreamWriter(stream);
            else
                writer = new OutputStreamWriter(stream, charset);

            writeProperties(properties, writer, separator);
        }
        finally
        {
            closeWriter(writer);
        }
    }    
}