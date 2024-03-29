/**
 * Copyright � 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.util.content;


/**
 * A class representing the attributes of a content object (such as a file or folder).
 *
 * @author Denis Evoy
 */
public class Attributes
{
    private boolean isHidden = false;                       //indicate whether or not the object is hidden
    private boolean isSystem = false;                       //indicate whether or not the object belongs to the system/OS
    private boolean isArchived = false;                     //indicate whether or not the object can be archived
    private boolean isDeletable = false;                    //indicate whether or not the object can be deleted
    private boolean isWritable = false;                     //indicate whether or not the contents of the object can be written
    private boolean isReadable = false;                     //indicate whether or not the contents of the object can be read
    private boolean isExecutable = false;                   //indicate whether or not the contents of the object can be executed by the system/OS


    /** Creates a set of default attributes. */
    public Attributes()
    {
        this(false, false, false, true, true, true, false);
    }
    
    /**
     * Creates a new set of attributes.
     * 
     * @param hidden      set to <code>true</code> if the file is hidden.
     * @param system      set to <code>true</code> if the file is a system file.
     * @param archived    set to <code>true</code> if the file can be archived.
     * @param deletable   set to <code>true</code> if the file can be deleted.
     * @param writable    set to <code>true</code> if the file contents can be written.
     * @param readable    set to <code>true</code> if the file contents can be read.
     * @param executable  set to <code>true</code> if the file contents can be executed by the OS.
     */
    public Attributes(boolean hidden, boolean system, boolean archived, boolean deletable, boolean writable, boolean readable, boolean executable)
    {
        isHidden = hidden;
        isSystem = system;
        isArchived = archived;
        isDeletable = deletable;
        isWritable = writable;
        isReadable = readable;
        isExecutable = executable;
    }


    /**
     * Returns whether or not the file is hidden.
     * 
     * @return <code>true</code> if the file is hidden.
     */
    public boolean isHidden()
    {
        return isHidden;
    }

    /**
     * Sets whether or not the file is hidden.
     * 
     * @param hidden <code>true</code> if the file is hidden.
     */
    public void setHidden(boolean hidden)
    {
        isHidden = hidden;
    }


    /**
     * Returns whether or not the file is a system file.
     * 
     * @return <code>true</code> if the file is a system file.
     */
    public boolean isSystem()
    {
        return isSystem;
    }

    /**
     * Sets whether or not the file is a system file.
     * 
     * @param system set to <code>true</code> if the file is a system file.
     */
    public void setSystem(boolean system)
    {
        isSystem = system;
    }


    /**
     * Returns whether or not the file can be archived.
     * 
     * @return <code>true</code> if the file can be archived.
     */
    public boolean isArchived()
    {
        return isArchived;
    }

    /**
     * Sets whether or not the file can be archived.
     * 
     * @param archived set to <code>true</code> if the file can be archived.
     */
    public void setArchived(boolean archived)
    {
        isArchived = archived;
    }


    /**
     * Returns whether or not the file can be deleted.
     * 
     * @return <code>true</code> if the file can be deleted.
     */
    public boolean isDeletable()
    {
        return isDeletable;
    }

    /**
     * Sets whether or not the file can be deleted.
     * 
     * @param deletable set to <code>true</code> if the file can be deleted.
     */
    public void setDeletable(boolean deletable)
    {
        isDeletable = deletable;
    }


    /**
     * Returns whether or not the file contents can be written.
     * 
     * @return set to <code>true</code> if the file contents can be written.
     */
    public boolean isWritable()
    {
        return isWritable;
    }

    /**
     * Sets whether or not the file contents can be written.
     * 
     * @param writable set to <code>true</code> if the file contents can be written.
     */
    public void setWritable(boolean writable)
    {
        isWritable = writable;
    }


    /**
     * Returns whether or not the file contents can be read.
     * 
     * @return set to <code>true</code> if the file contents can be read.
     */
    public boolean isReadable()
    {
        return isReadable;
    }

    /**
     * Sets whether or not the file contents can be read.
     * 
     * @param readable set to <code>true</code> if the file contents can be read.
     */
    public void setReadable(boolean readable)
    {
        isReadable = readable;
    }


    /**
     * Returns whether or not the file contents can be executed.
     * 
     * @return set to <code>true</code> if the file contents can be executed.
     */
    public boolean isExecutable()
    {
        return isExecutable;
    }

    /**
     * Sets whether or not the file contents can be executed.
     * 
     * @param executable set to <code>true</code> if the file contents can be executed.
     */
    public void setExecutable(boolean executable)
    {
        isExecutable = executable;
    }
}
