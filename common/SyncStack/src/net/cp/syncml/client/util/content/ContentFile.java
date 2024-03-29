/**
 * Copyright � 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.util.content;


import net.cp.syncml.client.devinfo.*;


/**
 * A class representing a file for the purposes of data sync. <br/><br/>
 * 
 * The following definition conforms to the following specifications:
 * <ul>
 *      <li> File data object specification Approved Version 1.2.1 � 10 August 2007
 * </ul>
 *
 * @author Denis Evoy
 */
public class ContentFile extends ContentObject
{
    /** Defines an extension containing the MD5 hash of the contents of a file. */
    public static final String EXT_CP_HASH_CONTENT =    "X-CP-HashContent";

    /** Defines an extension containing the display name of a file or folder. */
    public static final String EXT_CP_DISPLAY_NAME =    "X-CP-DisplayName";

    /** Defines an extension containing the unique ID of the memory card (if any) where the file/folder resides. */
    public static final String EXT_CP_MEM_CARD_ID =     "X-CP-MemoryCardId";

    /** Defines an extension which indicates (with values "true" or "false") if files can be created in a folder. */
    public static final String EXT_CP_NO_FILE_CHILD =   "X-CP-NoFileChild";

    /** Defines an extension which indicates (with values "true" or "false") if sub-folders can be created in a folder. */
    public static final String EXT_CP_NO_FOLDER_CHILD = "X-CP-NoFolderChild";

    
    ContentType fileContentType;                        //the MIME type of the content of the file
    long fileSize;                                      //the size of the file in bytes

    
    /** Creates an empty file. */
    ContentFile()
    {
        super();
    }
    
    /** 
     * Creates a file with the specified name.
     * 
     *  @param name the name of the file. May not be null or empty.
     */
    public ContentFile(String name)
    {
        super(name);
    }


    /**
     * Returns the content type of the data contained in the file.
     * 
     * @return The content type of the data contained in the file. May be null.
     */
    public ContentType getContentType()
    {
        return fileContentType;
    }

    /**
     * Sets the content type of the data contained in the file.
     * 
     * @param contentType the content type of the data contained in the file. May be null.
     */
    public void setContentType(ContentType contentType)
    {
        fileContentType = contentType;
    }


    /**
     * Returns the size of the file in bytes.
     * 
     * @return The size of the file in bytes. Will be zero or positive.
     */
    public long getSize()
    {
        return fileSize;
    }

    /**
     * Sets the size of the file in bytes.
     * 
     * @param size the size of the file in bytes. Must be zero or positive.
     */
    public void setSize(long size)
    {
        if (size < 0)
            throw new IllegalArgumentException("invalid size specified: " + size);

        fileSize = size;
    }
}
