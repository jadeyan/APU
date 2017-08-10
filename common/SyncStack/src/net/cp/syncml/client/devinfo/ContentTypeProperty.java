/**
 * Copyright © 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.devinfo;


/**
 * A class encapsulating a property associated with a content type.
 * 
 * @see ContentTypeCapabilities#getProperties()
 *
 * @author Denis Evoy
 */
public class ContentTypeProperty
{
    /**
     * A class encapsulating a parameter of a property associated with a content type.
     * 
     * @see ContentTypeProperty#getParameters()
     *
     * @author Denis Evoy
     */
    public static class Parameter
    {
        private String paramName;                           //the name of the parameter
        private String paramDisplayName;                    //the display name of the parameter
        private String paramDataType;                       //the data type of the parameter
        private int paramMaxSize;                           //the maximum size of the parameter
        private String[] paramEnumValues;                   //the supported enumerated values of the parameter

        
        /**
         * Create a new parameter with the specified name.
         * 
         * @param name the name of the parameter. Must not be null or empty.
         */
        public Parameter(String name)
        {
            this(name, null, null);
        }

        /**
         * Create a new parameter with the specified name and display name.
         * 
         * @param name          the name of the parameter. Must not be null or empty.
         * @param displayName   the display name of the parameter. May be null or empty.
         */
        public Parameter(String name, String displayName)
        {
            this(name, displayName, null);
        }
        
        /**
         * Create a new parameter with the specified name, display name and enumerated values.
         * 
         * @param name          the name of the parameter. Must not be null or empty.
         * @param displayName   the display name of the parameter. May be null or empty.
         * @param enumValues    the enumerated values of the parameter. May be null or empty.
         */
        public Parameter(String name, String displayName, String[] enumValues)
        {
            if ( (name == null) || (name.length() <= 0) )
                throw new IllegalArgumentException("no parameter name specified");

            paramName = name;
            paramDisplayName = displayName;
            paramEnumValues = enumValues;
        }
        
        /**
         * Create a new parameter with the specified name, display name and data type/size.
         * 
         * @param name          the name of the parameter. Must not be null or empty.
         * @param displayName   the display name of the parameter. May be null or empty.
         * @param dataType      the {@link ContentTypeProperty#DATA_TYPE_BINARY data type} of the parameter. May be null or empty.
         * @param maxSize       the maximum size of the parameter. Must be zero or positive.
         */
        public Parameter(String name, String displayName, String dataType, int maxSize)
        {
            if ( (name == null) || (name.length() <= 0) )
                throw new IllegalArgumentException("no parameter name specified");
            if (maxSize <= 0)
                throw new IllegalArgumentException("invalid max size specified: " + maxSize);

            paramName = name;
            paramDisplayName = displayName;
            paramDataType = dataType;
            paramMaxSize = maxSize;
        }

        
        /**
         * Returns the name of the parameter.
         * 
         * @return The name of the parameter. Will not be null or empty.
         */
        public String getName()
        {
            return paramName;
        }
        
        /**
         * Returns the display name of the parameter.
         * 
         * @return The display name of the parameter. May be null or empty.
         */
        public String getDisplayName()
        {
            return paramDisplayName;
        }
        
        /**
         * Returns the {@link ContentTypeProperty#DATA_TYPE_BINARY data type} of the parameter.
         * 
         * @return The data type of the parameter. May be null or empty.
         */
        public String getDataType()
        {
            return paramDataType;
        }
        
        /**
         * Returns the maximum size in UTF-8 characters of a parameter value.
         * 
         * @return The maximum size of the parameter or 0 if not set. Will be zero or positive.
         */
        public int getMaxSize()
        {
            return paramMaxSize;
        }
        
        /**
         * Returns the enumerated values associated with the parameter.
         * 
         * @return The supported enumerated values. May be null or empty.
         */
        public String[] getEnumValues()
        {
            return paramEnumValues;
        }
    }
    
    
    /** Defines a character data type. */
    public static final String DATA_TYPE_CHAR =        "chr";

    /** Defines an integer data type. */
    public static final String DATA_TYPE_INT =         "int";

    /** Defines a boolean data type. */
    public static final String DATA_TYPE_BOOLEAN =     "bool";

    /** Defines a binary data type. */
    public static final String DATA_TYPE_BINARY =      "bin";

    /** Defines a date/time data type. */
    public static final String DATA_TYPE_DATETIME =    "datetime";

    /** Defines a phone number data type. */
    public static final String DATA_TYPE_PHONENUM =    "phonenum";

    
    private String propName;                                //the name of the property
    private String propDisplayName;                         //the display name of the property
    private String propDataType;                            //the data type of the property
    private int propMaxSize;                                //the maximum size of the property
    private int propMaxOccurance;                           //the maximum number of occurrences of a property that are allowed
    private boolean propNoTruncate;                         //indicates if truncation is permitted when a property value exceed the maximum size
    private String[] propEnumValues;                        //the supported enumerated values of the property
    private Parameter[] propParams;                         //the supported parameters of the property

    
    /**
     * Create a new property with the specified name.
     * 
     * @param name  the name of the property. Must not be null or empty.
     */
    public ContentTypeProperty(String name)
    {
        this(name, null, null, null);
    }

    /**
     * Create a new property with the specified name and display name.
     * 
     * @param name          the name of the property. Must not be null or empty.
     * @param displayName   the display name of the property. May be null or empty.
     */
    public ContentTypeProperty(String name, String displayName)
    {
        this(name, displayName, null, null);
    }
    
    /**
     * Create a new property with the specified name, display name and enumerated values.
     * 
     * @param name          the name of the property. Must not be null or empty.
     * @param displayName   the display name of the property. May be null or empty.
     * @param enumValues    the enumerated values of the property. May be null or empty.
     */
    public ContentTypeProperty(String name, String displayName, String[] enumValues)
    {
        this(name, displayName, enumValues, null);
    }
    
    /**
     * Create a new property with the specified name, display name, enumerated values and parameters.
     * 
     * @param name          the name of the property. Must not be null or empty.
     * @param displayName   the display name of the property. May be null or empty.
     * @param enumValues    the enumerated values of the property. May be null or empty.
     * @param parameters    the parameters of the property. May be null or empty.
     */
    public ContentTypeProperty(String name, String displayName, String[] enumValues, Parameter[] parameters)
    {
        if ( (name == null) || (name.length() <= 0) )
            throw new IllegalArgumentException("no property name specified");

        propName = name;
        propDisplayName = displayName;
        propEnumValues = enumValues;
        propParams = parameters;
    }

    /**
     * Create a new property with the specified name, display name and data type/size.
     * 
     * @param name          the name of the property. Must not be null or empty.
     * @param displayName   the display name of the property. May be null or empty.
     * @param dataType      the {@link ContentTypeProperty#DATA_TYPE_BINARY data type} of the property. May be null or empty.
     * @param maxSize       the maximum size of the property. Must be zero or positive.
     * @param noTruncate    indicates if truncation should be permitted when a property value exceed the maximum size.
     * @param maxOccurance  the maximum number of occurrences of a property that are allowed. Must be zero or positive.
     */
    public ContentTypeProperty(String name, String displayName, String dataType, int maxSize, boolean noTruncate, int maxOccurance)
    {
        this(name, displayName, dataType, maxSize, noTruncate, maxOccurance, null);        
    }

    /**
     * Create a new property with the specified name, display name, data type/size and parameters.
     * 
     * @param name          the name of the property. Must not be null or empty.
     * @param displayName   the display name of the property. May be null or empty.
     * @param dataType      the {@link ContentTypeProperty#DATA_TYPE_BINARY data type} of the property. May be null or empty.
     * @param maxSize       the maximum size of the property. Must be zero or positive.
     * @param noTruncate    indicates if truncation should be permitted when a property value exceed the maximum size.
     * @param maxOccurance  the maximum number of occurrences of a property that are allowed. Must be zero or positive.
     * @param parameters    the parameters of the property. May be null or empty.
     */
    public ContentTypeProperty(String name, String displayName, String dataType, int maxSize, boolean noTruncate, int maxOccurance, Parameter[] parameters)
    {
        if ( (name == null) || (name.length() <= 0) )
            throw new IllegalArgumentException("no property name specified");
        if (maxSize < 0)
            throw new IllegalArgumentException("invalid max size specified: " + maxSize);
        if (maxOccurance < 0)
            throw new IllegalArgumentException("invalid max occurance specified: " + maxOccurance);

        propName = name;
        propDisplayName = displayName;
        propDataType = dataType;
        propMaxSize = maxSize;
        propNoTruncate = noTruncate;
        propMaxOccurance = maxOccurance;
        propParams = parameters;
    }
    
    
    /**
     * Returns the name of the property.
     * 
     * @return The name of the property. Will not be null or empty.
     */
    public String getName()
    {
        return propName;
    }
    
    /**
     * Returns the display name of the property.
     * 
     * @return The display name of the property. May be null or empty.
     */
    public String getDisplayName()
    {
        return propDisplayName;
    }
    
    /**
     * Returns the {@link ContentTypeProperty#DATA_TYPE_BINARY data type} of the property.
     * 
     * @return The data type of the property. May be null or empty.
     */
    public String getDataType()
    {
        return propDataType;
    }
    
    /**
     * Returns the maximum size in UTF-8 characters of a property value.
     * 
     * @return The maximum size of the property or 0 if not set. Will be zero or positive.
     */
    public int getMaxSize()
    {
        return propMaxSize;
    }
    
    /**
     * Returns whether or not truncation is permitted when a property value exceed the maximum size.
     * 
     * @return <code>true</code> if truncation is permitted when a property value exceed the maximum size.
     */
    public boolean isNoTruncate()
    {
        return propNoTruncate;
    }
    
    /**
     * Returns the maximum number of occurrences of a property that are allowed.
     * 
     * @return The the maximum number of occurrences of a property that are allowed or 0 if not set. Will be zero or positive.
     */
    public int getMaxOccurance()
    {
        return propMaxOccurance;
    }
    
    /**
     * Returns the enumerated values associated with the property.
     * 
     * @return The supported enumerated values. May be null or empty.
     */
    public String[] getEnumValues()
    {
        return propEnumValues;
    }
    
    /**
     * Returns the parameters associated with the property.
     * 
     * @return The supported parameters. May be null or empty.
     */
    public Parameter[] getParameters()
    {
        return propParams;
    }
}
