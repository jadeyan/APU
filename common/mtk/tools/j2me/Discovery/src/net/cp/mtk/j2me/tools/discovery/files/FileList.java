/**
 * Copyright © 2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.mtk.j2me.tools.discovery.files;


import java.io.*;

import java.util.*;

import javax.microedition.io.*;
import javax.microedition.io.file.*;
import javax.microedition.lcdui.*;

import net.cp.mtk.common.io.StreamUtils;
import net.cp.mtk.j2me.tools.discovery.DiscoveryMIDlet;


public class FileList extends List implements CommandListener
{
    public static interface SelectListener
    {
        public void onFileSelected(String selectedUrl);
    }


    private static final String[] FILE_TYPES =  { "File", "Directory" };

    private static final String UP_DIRECTORY =  "..";
    private static final String SEP_STR =       "/";
    private static final char SEP =             '/';
    
    
    private DiscoveryMIDlet midlet;
    private SelectListener selectListener;
    private Displayable parentScreen;
    private String currentDirUrl;

    private Command cmdView = new Command("View", Command.ITEM, 1);
    private Command cmdChoose = new Command("Choose", Command.OK, 2);
    private Command cmdProperties = new Command("Properties", Command.ITEM, 3);
    private Command cmdNew = new Command("New", Command.ITEM, 4);
    private Command cmdDelete = new Command("Delete", Command.ITEM, 5);
    private Command cmdNewOk = new Command("OK", Command.OK, 1);
    private Command cmdBack = new Command("Back", Command.BACK, 2);
    private TextField nameInput;
    private ChoiceGroup typeInput;
    private Image dirIcon;
    private Image fileIcon;
    private Image[] iconList;

    
    public FileList(String title, DiscoveryMIDlet midlet, Displayable parent, SelectListener listener)
    {
        this(title, midlet, parent, listener, null);
    }

    public FileList(String title, DiscoveryMIDlet midlet, Displayable parent, SelectListener listener, String dirUrl)
    {
        super(title, Choice.IMPLICIT);
        this.midlet = midlet;
        this.parentScreen = parent;
        this.selectListener = listener;

        dirIcon = null;
        fileIcon = null;
        try
        {
            dirIcon = Image.createImage("/dir.png");
            fileIcon = Image.createImage("/file.png");
        }
        catch (Throwable e)
        {
            //can't load icons
            dirIcon = null;
            fileIcon = null;
        }
        iconList = new Image[] { fileIcon, dirIcon };

        //add commands
        if (parent != null)
            addCommand(cmdBack);
        //addCommand(cmdView);
        setSelectCommand(cmdView);
        addCommand(cmdProperties);
        if (selectListener != null)
            addCommand(cmdChoose);
        setCommandListener(this);
        
        //list the files in the specified directory - if that fails, just list the root directories
        currentDirUrl = null;
        if (! listFiles(dirUrl, true))
            listFiles(null, false);
    }

    private void listFiles()
    {
        listFiles(currentDirUrl, false);
    }

    private boolean listFiles(String dirUrl, boolean hideErrors)
    {
        if ( (dirUrl == null) || (dirUrl.length() <= 0) )
            dirUrl = "file:///";

        FileConnection currentDir = null;
        boolean showRoots = (dirUrl.equalsIgnoreCase("file:///"));
        try
        {
            //get the list of files in the specified directory
            Enumeration fileEnum;
            if (showRoots)
            {
                //get list of root directories
                fileEnum = FileSystemRegistry.listRoots();
            }
            else
            {
                //get list of all files in the specified directory
                currentDir = (FileConnection)Connector.open(dirUrl);
                fileEnum = currentDir.list("*", true);
            }

            deleteAll();
            if (! showRoots)
                append(UP_DIRECTORY, dirIcon);

            //add each file to the list
            while (fileEnum.hasMoreElements())
            {
                String fileName = (String)fileEnum.nextElement();
                if (fileName.charAt(fileName.length() - 1) == SEP)
                    append(fileName, dirIcon);
                else
                    append(fileName, fileIcon);
            }

            if (! showRoots)
            {
                addCommand(cmdNew);
                addCommand(cmdDelete);
            }
            else
            {
                removeCommand(cmdNew);
                removeCommand(cmdDelete);
            }
            
            currentDirUrl = dirUrl;
            return true;
        }
        catch (Throwable e)
        {
            if (! hideErrors)
                midlet.showError("Cannot list files in folder '" + dirUrl + "'", e);
            return false;
        }
    }
    
    public void commandAction(Command command, Displayable displayable)
    {
        if (displayable == this)
        {
            //get the name of the selected file (if any)
            String selectedFileName = null;
            int selectedIndex = getSelectedIndex();
            if (selectedIndex >= 0)
                selectedFileName = getString(selectedIndex);
            
            if (command == cmdView)
            {
                viewFile(selectedFileName);
            }
            else if (command == cmdChoose)
            {
                if ( (selectListener != null) && (selectedFileName != null) && (! selectedFileName.equals(UP_DIRECTORY)) )
                    selectListener.onFileSelected(currentDirUrl + selectedFileName);
            }
            else if (command == cmdProperties)
            {
                showProperties(selectedFileName);
            }
            else if (command == cmdDelete)
            {
                deleteFile(selectedFileName);
            }
            else if (command == cmdNew)
            {
                newFile();
            }
            else if (command == cmdBack)
            {
                if (parentScreen != null)
                    Display.getDisplay(midlet).setCurrent(parentScreen);
            }
        }
        else
        {
            if (command == cmdNewOk)
            {
                newFileConfirmed();
            }
            else if (command == cmdBack)
            {
                Display.getDisplay(midlet).setCurrent(this);
            }
        }
    }

    private void viewFile(String fileName)
    {
        final String name = fileName;
        new Thread(new Runnable()
        {
            public void run()
            {
                if ( (name.endsWith(SEP_STR)) || (name.equals(UP_DIRECTORY)) )
                    showDirectory(name);
                else
                    showFile(name);
            }
        }).start();
    }

    private void showDirectory(String fileName)
    {
        String newDirectoryUrl = null;
        if (currentDirUrl == null)
        {
            //go to the specified root
            if (fileName.equals(UP_DIRECTORY))
                return;
            newDirectoryUrl = fileName;
        }
        else if (fileName.equals(UP_DIRECTORY))
        {
            //go up one directory
            int index = currentDirUrl.lastIndexOf(SEP, (currentDirUrl.length() - 2));
            if (index != -1)
                newDirectoryUrl = currentDirUrl.substring(0, index + 1);
        }
        else
        {
            //go down to the specified directory
            newDirectoryUrl = currentDirUrl + fileName;
        }

        //list the files in the new directory
        listFiles(newDirectoryUrl, false);
    }

    private void showFile(String fileName)
    {
        FileConnection fileConn = null;
        InputStream fileStream = null;
        try
        {
            fileConn = (FileConnection)Connector.open(currentDirUrl + fileName, Connector.READ);
            if (! fileConn.exists())
                throw new IOException("File does not exist");
            
            int previewSize = (1024 * 5);
            TextBox viewer = new TextBox(fileName, null, previewSize, TextField.ANY | TextField.UNEDITABLE);
            viewer.setTitle("Preview");

            //read the data
            fileStream = fileConn.openInputStream();
            byte[] fileData = new byte[previewSize];
            int bytesRead = fileStream.read(fileData);
            if (bytesRead > 0)
                viewer.setString(new String(fileData, 0, bytesRead));

            //display the viewer
            viewer.addCommand(cmdBack);
            viewer.setCommandListener(this);
            Display.getDisplay(midlet).setCurrent(viewer);
        }
        catch (Throwable e)
        {
            midlet.showError("Cannot show preview of file '" + fileName + "'", e);
        }
        finally
        {
            StreamUtils.closeStream(fileStream);
            FileSystem.closeFile(fileConn);
        }
    }

    void showProperties(String fileName)
    {
        if (fileName.equals(UP_DIRECTORY))
            return;

        FileConnection fileConn = null;
        try
        {
            fileConn = (FileConnection)Connector.open(currentDirUrl + fileName, Connector.READ);
            if (! fileConn.exists())
                throw new IOException("File does not exist");

            Form propertiesForm = new Form("Properties");
            DiscoveryMIDlet.addText(propertiesForm, "URL: ", fileConn.getURL());

            DiscoveryMIDlet.addEmptyLine(propertiesForm);
            DiscoveryMIDlet.addText(propertiesForm, "Path: ", fileConn.getPath());
            DiscoveryMIDlet.addText(propertiesForm, "Name: ", fileConn.getName());
            DiscoveryMIDlet.addText(propertiesForm, "Type: ", fileConn.isDirectory() ? "Directory" : "File");
            
            DiscoveryMIDlet.addEmptyLine(propertiesForm);
            if (fileConn.isDirectory())
            {
                DiscoveryMIDlet.addText(propertiesForm, "Size: ", fileConn.directorySize(false) + " Bytes");
                DiscoveryMIDlet.addText(propertiesForm, "Size (All): ", fileConn.directorySize(true) + " Bytes");
            }
            else
            {
                DiscoveryMIDlet.addText(propertiesForm, "Size: ", fileConn.fileSize() + " Bytes");
            }
            DiscoveryMIDlet.addText(propertiesForm, "Modified: ", DiscoveryMIDlet.dateToString(fileConn.lastModified()));
            
            DiscoveryMIDlet.addEmptyLine(propertiesForm);
            String name = fileConn.getName();
            if ( (name == null) || (name.length() <= 0) )
            {
                //show additional properties for the file system root 
                DiscoveryMIDlet.addText(propertiesForm, "Total: ", fileConn.totalSize()+ " Bytes");
                DiscoveryMIDlet.addText(propertiesForm, "Used: ", fileConn.usedSize()+ " Bytes");
                DiscoveryMIDlet.addText(propertiesForm, "Free: ", fileConn.availableSize() + " Bytes");
            }

            DiscoveryMIDlet.addEmptyLine(propertiesForm);
            DiscoveryMIDlet.addText(propertiesForm, "Readable: ", (fileConn.canRead() ? "Yes" : "No"));
            DiscoveryMIDlet.addText(propertiesForm, "Writable: ", (fileConn.canWrite() ? "Yes" : "No"));
            DiscoveryMIDlet.addText(propertiesForm, "Hidden: ", (fileConn.isHidden() ? "Yes" : "No"));

            propertiesForm.addCommand(cmdBack);
            propertiesForm.setCommandListener(this);
            Display.getDisplay(midlet).setCurrent(propertiesForm);
        }
        catch (Throwable e)
        {
            midlet.showError("Cannot show properties of file/folder '" + fileName + "'", e);
        }
        finally
        {
            FileSystem.closeFile(fileConn);
        }
    }

    private void deleteFile(String fileName)
    {
        if (fileName.equals(UP_DIRECTORY))
            return;
        
        FileConnection fileConn = null;
        try
        {
            fileConn = (FileConnection)Connector.open(currentDirUrl + fileName);
            if (fileConn.isDirectory())
            {
                //make sure the directory is empty
                Enumeration content = fileConn.list("*", true);
                if (content.hasMoreElements())
                {
                    midlet.showError("Cannot delete non-empty folder '" + fileName + "'", null);
                    return;
                }
            }

            //delete the file
            fileConn.delete();
            
            //refresh the list of files
            listFiles();
            Display.getDisplay(midlet).setCurrent(this);
        }
        catch (Throwable e)
        {
            midlet.showError("Cannot delete file/folder '" + fileName + "'", null);
        }
        finally
        {
            FileSystem.closeFile(fileConn);
        }
    }

    void newFile()
    {
        //prompt the user for the name and type
        Form newFileForm = new Form("New File");
        nameInput = new TextField("Name:", null, 256, TextField.ANY);
        newFileForm.append(nameInput);
        
        DiscoveryMIDlet.addEmptyLine(newFileForm);
        typeInput = new ChoiceGroup("Type:", Choice.EXCLUSIVE, FILE_TYPES, iconList);
        newFileForm.append(typeInput);
        
        newFileForm.addCommand(cmdNewOk);
        newFileForm.addCommand(cmdBack);
        newFileForm.setCommandListener(this);
        Display.getDisplay(midlet).setCurrent(newFileForm);
    }

    private void newFileConfirmed()
    {
        //validate user input
        String newName = nameInput.getString();
        boolean isDirectory = (typeInput.getSelectedIndex() != 0);
        if ( (newName == null) || (newName.length() <= 0) )
        {
            midlet.showError("File Name is empty. Please provide a valid file name.", null);
            return;
        }

        FileConnection fileConn = null;
        try
        {
            //create the file
            fileConn = (FileConnection)Connector.open(currentDirUrl + newName);
            if (isDirectory)
                fileConn.mkdir();
            else
                fileConn.create();

            //refresh the list of files
            listFiles();
            Display.getDisplay(midlet).setCurrent(this);
        }
        catch (Throwable e)
        {
            midlet.showError("Cannot create file/folder '" + newName + "'", e);
        }
        finally
        {
            FileSystem.closeFile(fileConn);
        }
    }
}
