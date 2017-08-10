/**
 * Copyright © 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.devinfo;


/**
 * A class representing a non-standard experimental extension.
 * 
 * @see DeviceCapabilities#getExtensions()
 *
 * @author Denis Evoy
 */
public class Extension
{
    private String extensionName;
    private String[] extensionValues;

    
    /**
     * Creates a new extension with the specified name.
     * 
     * @param name the name of the extension. Must not be null or empty.
     */
    public Extension(String name)
    {
        this(name, (String[])null);
    }

    /**
     * Creates a new extension with the specified name and value.
     * 
     * @param name  the name of the extension. Must not be null or empty.
     * @param value the value of the extension. Must not be null or empty.
     */
    public Extension(String name, String value)
    {
        this(name, (String[])null);

        if ( (value == null) || (value.length() <= 0) )
            throw new IllegalArgumentException("no extension value specified");
        
        extensionValues = new String[] { value };
    }

    /**
     * Creates a new extension with the specified name and values.
     * 
     * @param name      the name of the extension. Must not be null or empty.
     * @param values    the values of the extension. May be null or empty.
     */
    public Extension(String name, String[] values)
    {
        if ( (name == null) || (name.length() <= 0) )
            throw new IllegalArgumentException("no extension name specified");

        extensionName = name;
        extensionValues = values;
    }


    /**
     * Returns the name of the extension.
     * 
     * @return The name of the extension.
     */
    public String getName()
    {
        return extensionName;
    }

    /**
     * Returns the values of the extension.
     * 
     * @return The values of the extension. May be null or empty.
     */
    public String[] getValues()
    {
        return extensionValues;
    }
}
