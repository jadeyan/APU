/**
 * Copyright © 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.devinfo;


/**
 * A class encapsulating a content type. <br/><br/>
 * 
 * A content refers to a MIME type usually specified in the form:
 * <pre>
 *      &lt;MainType&gt;/&lt;SubType&gt;
 * </pre>
 * 
 * In addition to the standard main type and sub type details, this class also allows
 * a version number to be associated with a content type.
 *
 * @author Denis Evoy
 */
public class ContentType
{
    /** Defines a content type representing a vCard v2.1 contact. */
    public static final ContentType CT_CONTACT_2_1 =     new ContentType("text", "x-vcard", "2.1");

    /** Defines a content type representing a vCard v3.0 contact. */
    public static final ContentType CT_CONTACT_3_0 =     new ContentType("text", "vcard", "3.0");
    
    /** Defines a content type representing a vCalendar 1.0 calendar. */
    public static final ContentType CT_CALENDAR_1_0 =    new ContentType("text", "x-vcalendar", "1.0");

    /** Defines a content type representing a iCalendar 2.0 calendar. */
    public static final ContentType CT_CALENDAR_2_0 =    new ContentType("text", "calendar", "2.0");

    /** Defines a content type representing a file. */
    public static final ContentType CT_FILE =            new ContentType("application", "vnd.omads-file", "1.0");

    /** Defines a content type representing a folder. */
    public static final ContentType CT_FOLDER =          new ContentType("application", "vnd.omads-folder", "1.0");
    
    
    private String ctMainType;                      //the main type part of the content type (e.g. audio, image, etc)
    private String ctSubType;                       //the sub type part of the content type (e.g. jpeg, wav, etc)
    private String ctVersion;                       //the version of the content type (if any)

    
    /**
     * Returns whether or not the specified type is a valid content type.
     * 
     * @param contentType the content type in string form.
     * @return <code>true</code> if the content type is validly formed.
     */
    public static boolean isValidContentType(String contentType)
    {
        try
        {
            new ContentType(contentType);
        }
        catch (IllegalArgumentException e)
        {
            return false;
        }
        
        return true;
    }
    
    
    /**
     * Creates a new content type with the specified type. <br/><br/>
     * 
     * The specified content type must be in the form:
     * <pre>
     *      &lt;MainType&gt;/&lt;SubType&gt;
     * </pre>
     * 
     * @param contentType the content type in string form. Must be a validly formed MIME type.
     */
    public ContentType(String contentType)
    {
        if ( (contentType == null) || (contentType.length() <= 0) )
            throw new IllegalArgumentException("no content type specified");
        
        int index = contentType.indexOf("/");
        if ( (index <= 0) || (index >= (contentType.length() - 1)) )
            throw new IllegalArgumentException("invalid content type specified: " + contentType);
        
        ctMainType = contentType.substring(0, index);
        ctSubType = contentType.substring(index + 1);
    }

    /**
     * Creates a new content type with the specified main type and sub type.
     * 
     * @param mainType  the main type part of the content type (audio, image, etc). Must not be null or empty.
     * @param subType   the sub type part of the content type (jpeg, wav, etc). Must not be null or empty.
     */
    public ContentType(String mainType, String subType)
    {
        this(mainType, subType, null);
    }

    /**
     * Creates a new content type with the specified main type, sub type type and version.
     * 
     * @param mainType  the main type part of the content type (audio, image, etc). Must not be null or empty.
     * @param subType   the sub type part of the content type (jpeg, wav, etc). Must not be null or empty.
     * @param version   the version of the content type. May be null or empty.
     */
    public ContentType(String mainType, String subType, String version)
    {
        if ( (mainType == null) || (mainType.length() <= 0) )
            throw new IllegalArgumentException("no main type specified");
        if ( (subType == null) || (subType.length() <= 0) )
            throw new IllegalArgumentException("no sub type specified");

        ctMainType = mainType;
        ctSubType = subType;
        ctVersion = version;
    }

    
    /**
     * Returns the main type part of the content type.
     * 
     * @return The main type part of the content type. Will not be null or empty.
     */
    public String getMainType()
    {
        return ctMainType;
    }

    /**
     * Returns the sub type part of the content type.
     * 
     * @return The sub type part of the content type. Will not be null or empty.
     */
    public String getSubType()
    {
        return ctSubType;
    }

    /**
     * Returns the version of the content type.
     * 
     * @return The version of the content type. May be null or empty.
     */
    public String getVersion()
    {
        return ctVersion;
    }

    
    public boolean equals(Object obj)
    {
        if (! (obj instanceof ContentType))
            return false;
        
        ContentType contentType = (ContentType)obj;
        return ( (contentType.getMainType().equals(ctMainType)) && (contentType.getSubType().equals(ctSubType)) );
    }

    /**
     * Returns the string version of the content type. <br/><br/>
     * 
     * The returned string will be in the form:
     * <pre>
     *      &lt;MainType&gt;/&lt;SubType&gt;
     * </pre>
     * 
     * @return The string version of the content type.
     */
    public String toString()
    {
        return ctMainType + "/" + ctSubType;
    }
}
