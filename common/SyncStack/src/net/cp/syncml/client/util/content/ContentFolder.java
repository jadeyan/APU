/**
 * Copyright � 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.util.content;


/**
 * A class representing a folder for the purposes of data sync. <br/><br/>
 * 
 * The following definition conforms to the following specifications:
 * <ul>
 *      <li> Folder data object specification Approved Version 1.2.1 � 10 August 2007
 * </ul>
 *
 * @author Denis Evoy
 */
public class ContentFolder extends ContentObject
{
    /** Defines a role indicating that that folder contains inbound messages. */
    public static final String ROLE_INBOX =         "inbox";
    
    /** Defines a role indicating that that folder contains outgoing messages. */
    public static final String ROLE_OUTBOX =        "outbox";
    
    /** Defines a role indicating that that folder contains messages currently being edited. */
    public static final String ROLE_DRAFTS =        "drafts";
    
    /** Defines a role indicating that that folder contains messages that have been sent. */
    public static final String ROLE_SENT =          "sent";
    
    /** Defines a role indicating that that folder contains documents. */
    public static final String ROLE_DOCUMENTS =     "documents";
    
    /** Defines a role indicating that that folder contains pictures. */
    public static final String ROLE_PICTURES =      "pictures";
    
    /** Defines a role indicating that that folder contains movies. */
    public static final String ROLE_MOVIES =        "movies";
    
    /** Defines a role indicating that that folder contains music. */
    public static final String ROLE_MUSIC =         "music";
    
    /** Defines a role indicating that that folder contains applications. */
    public static final String ROLE_APPLICATIONS =  "applications";
    
    
    String folderRole;                                  //the role of the folder

    
    /** Creates an empty folder. */
    ContentFolder()
    {
        super();
    }

    /** 
     * Creates a folder with the specified name.
     * 
     *  @param name the name of the folder. May not be null or empty.
     */
    public ContentFolder(String name)
    {
        super(name);
    }


    /**
     * Returns the {@link #ROLE_APPLICATIONS role} of the folder.
     * 
     * @return The {@link #ROLE_APPLICATIONS role} of the folder. May be null or empty.
     */
    public String getRole()
    {
        return folderRole;
    }

    /**
     * Sets the {@link #ROLE_APPLICATIONS role} of the folder.
     * 
     * @param role the {@link #ROLE_APPLICATIONS role} of the folder. May be null or empty.
     */
    public void setRole(String role)
    {
        folderRole = role;
    }
}
