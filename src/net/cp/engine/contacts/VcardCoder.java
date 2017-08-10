/**
 * Copyright 2004-2009 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.engine.contacts;

import java.io.*;

import net.cp.syncml.client.devinfo.ContentType;
import net.cp.syncml.client.store.StoreException;
import net.cp.syncml.client.util.Logger;

/**
 * An abstract class defining a basic vCard encoder/decoder.
 *
 * @author  Denis Evoy
 */
public abstract class VcardCoder
{
    /* Definition of possible vCard versions */
    public static final String VERSION_2_1 =    "2.1";
    public static final String VERSION_3_0 =    "3.0";
    
    /**
     * the character set used when encoding/decoding
     */
    protected String charset;
    
    /**
     * the vCard version (2.1 or 3.0) to encode/decode
     */
    protected String version;
    
    /**
     * the logger to use to log activity
     */
    protected Logger logger;
    
    /**
     * the contact store using this VCard coder
     */
    protected ContactStore store;
    
    /**
     * the Vcard content type
     */
    protected ContentType contentType;

    
    /** Creates an encoder/decoder to process UTF-8, version 3.0 vCards. */
    public VcardCoder(ContactStore contactStore, Logger syncLogger)
    {
        this(contactStore, "UTF-8", VERSION_3_0, syncLogger);
    }

    /** Creates an encoder/decoder to process vCards in the specified character set and preferred vCard version. */
    public VcardCoder(ContactStore contactStore, String characterSet, String vcardVersion, Logger syncLogger)
    {
        if ( (characterSet == null) || (characterSet.length() <= 0) )
            throw new IllegalArgumentException("no character set specified");
        if ( (! vcardVersion.equals(VERSION_2_1)) && (! vcardVersion.equals(VERSION_3_0)) )
            throw new IllegalArgumentException("invalid vCard version '" + vcardVersion +  "' specified");
        
        charset = characterSet;
        version = vcardVersion;
        logger = syncLogger;
        store = contactStore;
        
        //the content type will be set later when we have established the actual vCard version we will be using
        contentType = null;
    }
    
    
    /** Return the supported vCard content type */
    public ContentType getContentType()
    {
        //determine the content type from the actual vCard version being used
    	if (contentType == null)
    	{
    		if (version.equals(VERSION_3_0))
		    	contentType = new ContentType("text", "vcard", "3.0");
		    else if (version.equals(VERSION_2_1))
		    	contentType = new ContentType("text", "x-vcard", "2.1");
		    else
		    	throw new IllegalArgumentException("Unsupported vcard version '" + version + "'");
    	}
    	
    	return contentType;
    }

    
    /** Returns an identifier that can be used by the user to identify the contact represented by the specified vCard. */
    public abstract String getContactIdentifier(String vcardString);
        
    /** Returns whether or not the specified field is supported by the coder. */
    public abstract boolean isFieldSupported(Contact contact, int fieldId);
   
    /** Reads the specified vCard data and returns it in a contact. */
    public abstract Contact decode(ContactList contactList, byte[] vcardData)
        throws StoreException;
        
    /** Reads the specified vCard data and returns it in a contact. */
    public abstract Contact decode(ContactList contactList, String vcardString)
        throws StoreException;
        
    /** Writes the specified contact as a vCard to the specified output stream. */
    public abstract void encode(Contact contact, OutputStream stream)
        throws StoreException;
}
