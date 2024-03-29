/**
 * Copyright � 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.engine;


import java.io.*;
import java.util.Stack;

import net.cp.syncml.client.util.Logger;
import net.cp.syncml.client.util.wbxml.*;


/**
 * A class defining a WBXML codepage for SyncML.
 * 
 * This codepage conforms with SyncML version 1.1 and 1.2.
 *
 * @author Denis Evoy
 */
public class SyncMLCodepage extends Codepage
{
    //SyncML DTD versions
    public static final String VER_DTD_1_1 =           "1.1";
    public static final String VER_DTD_1_2 =           "1.2";

    //SyncML protocol versions
    public static final String VER_PROTO_1_1 =         "SyncML/1.1";
    public static final String VER_PROTO_1_2 =         "SyncML/1.2";
    
    
    //SyncML DTD public IDs
    private static final String DOC_ID_1_1 =           "-//SYNCML//DTD SyncML 1.1//EN";
    private static final String DOC_ID_1_2 =           "-//SYNCML//DTD SyncML 1.2//EN";

    //The index of the codepage
    private static final int PAGE_INDEX =              0;
    
    //SyncML 1.1 codepage definition
    private static final byte TAG_ADD =                0x05;
    private static final byte TAG_ALERT =              0x06;
    private static final byte TAG_ARCHIVE =            0x07;
    private static final byte TAG_ATOMIC =             0x08;
    private static final byte TAG_CHAL =               0x09;
    private static final byte TAG_CMD =                0x0A;
    private static final byte TAG_CMD_ID =             0x0B;
    private static final byte TAG_CMD_REF =            0x0C;
    private static final byte TAG_COPY =               0x0D;
    private static final byte TAG_CRED =               0x0E;
    private static final byte TAG_DATA =               0x0F;
    private static final byte TAG_DELETE =             0x10;
    private static final byte TAG_EXEC =               0x11;
    private static final byte TAG_FINAL =              0x12;
    private static final byte TAG_GET =                0x13;
    private static final byte TAG_ITEM =               0x14;
    private static final byte TAG_LANG =               0x15;
    private static final byte TAG_LOC_NAME =           0x16;
    private static final byte TAG_LOC_URI =            0x17;
    private static final byte TAG_MAP =                0x18;
    private static final byte TAG_MAPITEM =            0x19;
    private static final byte TAG_META =               0x1A;
    private static final byte TAG_MSG_ID =             0x1B;
    private static final byte TAG_MSG_REF =            0x1C;
    private static final byte TAG_NO_RESP =            0x1D;
    private static final byte TAG_NO_RESULTS =         0x1E;
    private static final byte TAG_PUT =                0x1F;
    private static final byte TAG_REPLACE =            0x20;
    private static final byte TAG_RESP_URI =           0x21;
    private static final byte TAG_RESULTS =            0x22;
    private static final byte TAG_SEARCH =             0x23;
    private static final byte TAG_SEQUENCE =           0x24;
    private static final byte TAG_SESSION_ID =         0x25;
    private static final byte TAG_SOFT_DEL =           0x26;
    private static final byte TAG_SOURCE =             0x27;
    private static final byte TAG_SOURCE_REF =         0x28;
    private static final byte TAG_STATUS =             0x29;
    private static final byte TAG_SYNC =               0x2A;
    private static final byte TAG_SYNC_BODY =          0x2B;
    private static final byte TAG_SYNC_HEADER =        0x2C;
    private static final byte TAG_SYNCML =             0x2D;
    private static final byte TAG_TARGET =             0x2E;
    private static final byte TAG_TARGET_REF =         0x2F;
    private static final byte TAG_RESERVED =           0x30;     //reserved for future use
    private static final byte TAG_VER_DTD =            0x31;
    private static final byte TAG_VER_PROTO =          0x32;
    private static final byte TAG_NUMBER_OF_CHANGES =  0x33;
    private static final byte TAG_MORE_DATA =          0x34;
    
    //SyncML 1.2 codepage definition
    private static final byte TAG_FIELD =              0x35;
    private static final byte TAG_FILTER =             0x36;
    private static final byte TAG_RECORD =             0x37;
    private static final byte TAG_FILTER_TYPE =        0x38;
    private static final byte TAG_SOURCE_PARENT =      0x39;
    private static final byte TAG_TARGET_PARENT =      0x3A;
    private static final byte TAG_MOVE =               0x3B;

    private static final String[] TAG_NAMES =          { "Add", "Alert", "Archive", "Atomic", "Chal", "Cmd", "CmdID", "CmdRef", "Copy", "Cred", "Data", "Delete", "Exec", "Final", "Get", "Item", "Lang", "LocName", "LocURI", "Map", "MapItem", "Meta", "MsgID", "MsgRef", "NoResp", "NoResults", "Put", "Replace", "RespURI", "Results", "Search", "Sequence", "SessionID", "SftDel", "Source", "SourceRef", "Status", "Sync", "SyncBody", "SyncHdr", "SyncML", "Target", "TargetRef", "RESERVED", "VerDTD", "VerProto", "NumberOfChanges", "MoreData", "Field", "Filter", "Record", "FilterType", "SourceParent", "TargetParent", "Move" };
    
    //The encoding to use when handling strings
    private static final String ENCODING_UTF8 =        "UTF-8";
    
    
    private Session syncSession;                //the session in which the codepage is being used
    private MetInfCodepage cpMetinf;            //the codepage used to parse <Meta> information

    private SyncHdr inSyncHeader;               //the sync header information being parsed
    private Status inStatus;                    //the sync status command being parsed
    private Cmd inCommand;                      //the sync command being parsed
    private Stack inNestedCmds;                 //the stack containing any nested commands
    private SyncItem inItem;                    //the sync item being parsed
    private MapItem inMapItem;                  //the sync map item being parsed
    private ByteArrayOutputStream inDataStream; //the opaque data being parsed
    

    /**
     * Creates a new SyncML codepage for use in the specified session.
     * 
     * @param logger            the logger used to log activity. 
     * @param session           the session in which the codepage will be used.
     * @param metinfCodepage    the codepage used to encode/decode MetInf data.
     */
    public SyncMLCodepage(Logger logger, Session session, MetInfCodepage metinfCodepage)
    {
        super(logger);
        
        syncSession = session;
        cpMetinf = metinfCodepage;

        inNestedCmds = new Stack();
        inDataStream = new ByteArrayOutputStream(256);            
    }


    public String[] getTagNames()
    {
        return TAG_NAMES;
    }
    
    public void onTagStart(int tagId, boolean hasContent) 
        throws WbxmlException
    {
        super.onTagStart(tagId, hasContent);

        //determine the ID of the parent tag (if any)
        int parentId = getParentId();
        
        if (tagId == TAG_ADD)
        {
            //handle any existing command (to cater for nested commands)
            doParentCommand();
            
            //initialize the ADD command
            inCommand = new Cmd(Cmd.CMD_ADD);
        }
        else if (tagId == TAG_ALERT)
        {
            //handle any existing command (to cater for nested commands)
            doParentCommand();
            
            //initialize the ALERT command
            inCommand = new Cmd(Cmd.CMD_ALERT);
        }
        else if (tagId == TAG_ARCHIVE)
        {
            //indicate that an archived delete should be performed
            if (parentId == TAG_DELETE)
                inCommand.archivedDelete = true;
        }
        else if (tagId == TAG_ATOMIC)
        {
            //handle any existing command (to cater for nested commands)
            doParentCommand();
            
            //initialize the ATOMIC command
            inCommand = new Cmd(Cmd.CMD_ATOMIC);
            inNestedCmds.push(inCommand);
        }
        else if (tagId == TAG_CHAL)
        {
            //initialize the challenge information
            if (parentId == TAG_STATUS)
                inStatus.challenge = new Chal();
        }
        else if (tagId == TAG_COPY)
        {
            //handle any existing command (to cater for nested commands)
            doParentCommand();
            
            //initialize the COPY command
            inCommand = new Cmd(Cmd.CMD_COPY);
        }
        else if (tagId == TAG_CRED)
        {
            //initialize the credentials information
            if (parentId == TAG_SYNC_HEADER)
                inSyncHeader.credentials = new Cred();
            else if (parentId == TAG_STATUS)
                inStatus.credentials = new Cred();
            else if (isCommand(parentId))
                inCommand.credentials = new Cred();
        }
        else if (tagId == TAG_DELETE)
        {
            //handle any existing command (to cater for nested commands)
            doParentCommand();
            
            //initialize the DELETE command
            inCommand = new Cmd(Cmd.CMD_DELETE);
        }
        else if (tagId == TAG_EXEC)
        {
            //handle any existing command (to cater for nested commands)
            doParentCommand();
            
            //initialize the EXEC command
            inCommand = new Cmd(Cmd.CMD_EXEC);
        }
        else if (tagId == TAG_FINAL)
        {
            //handle the end of the package
            syncSession.onSyncPkgEnd();
        }
        else if (tagId == TAG_GET)
        {
            //handle any existing command (to cater for nested commands)
            doParentCommand();
            
            //initialize the GET command
            inCommand = new Cmd(Cmd.CMD_GET);
        }
        else if (tagId == TAG_ITEM)
        {
            //initialize the item
            inItem = new SyncItem();
        }
        else if (tagId == TAG_MAP)
        {
            //handle any existing command (to cater for nested commands)
            doParentCommand();
            
            //initialize the MAP command
            inCommand = new Cmd(Cmd.CMD_MAP);
        }
        else if (tagId == TAG_MAPITEM)
        {
            //initialize the map item
            inMapItem = new MapItem();
        }
        else if (tagId == TAG_META)
        {
            //clear the MetInf codepage
            cpMetinf.clear();
        }
        else if (tagId == TAG_NO_RESP)
        {
            //indicate that no response is required
            if (parentId == TAG_SYNC_HEADER)
                inSyncHeader.noResponse = true;
            else if (isCommand(tagId))
                inCommand.noResponse = true;
        }
        else if (tagId == TAG_NO_RESULTS)
        {
            //indicate that no search results are required
            if (parentId == TAG_SEARCH)
                inCommand.noResults = true;
        }
        else if (tagId == TAG_PUT)
        {
            //handle any existing command (to cater for nested commands)
            doParentCommand();
            
            //initialize the PUT command
            inCommand = new Cmd(Cmd.CMD_PUT);
        }
        else if (tagId == TAG_REPLACE)
        {
            //handle any existing command (to cater for nested commands)
            doParentCommand();
            
            //initialize the REPLACE command
            inCommand = new Cmd(Cmd.CMD_REPLACE);
        }
        else if (tagId == TAG_RESULTS)
        {
            //handle any existing command (to cater for nested commands)
            doParentCommand();
            
            //initialize the RESULTS command
            inCommand = new Cmd(Cmd.CMD_RESULTS);
        }
        else if (tagId == TAG_SEARCH)
        {
            //handle any existing command (to cater for nested commands)
            doParentCommand();
            
            //initialize the SEARCH command
            inCommand = new Cmd(Cmd.CMD_SEARCH);
        }
        else if (tagId == TAG_SEQUENCE)
        {
            //handle any existing command (to cater for nested commands)
            doParentCommand();
            
            //initialize the SEQUENCE command
            inCommand = new Cmd(Cmd.CMD_SEQUENCE);
            inNestedCmds.push(inCommand);
        }
        else if (tagId == TAG_SOFT_DEL)
        {
            //indicate that a soft delete is required
            if (parentId == TAG_DELETE)
                inCommand.softDelete = true;
        }
        else if (tagId == TAG_STATUS)
        {
            //initialize the status command 
            inStatus = new Status();
        }
        else if (tagId == TAG_SYNC)
        {
            //handle any existing command (to cater for nested commands)
            doParentCommand();
            
            //initialize the SYNC command
            inCommand = new Cmd(Cmd.CMD_SYNC);
            inNestedCmds.push(inCommand);
        }
        else if (tagId == TAG_SYNC_HEADER)
        {
            //initialize the sync header
            inSyncHeader = new SyncHdr();
        }
        else if (tagId == TAG_RESERVED)
        {
            //ignore
        }
        else if (tagId == TAG_MORE_DATA)
        {
            //indicate that more data will be sent for the item 
            if (parentId == TAG_ITEM)
                inItem.moreData = true;
        }
        else if (tagId == TAG_FIELD)
        {
            //not supported
        }
        else if (tagId == TAG_FILTER)
        {
            //not supported
        }
        else if (tagId == TAG_RECORD)
        {
            //not supported
        }
        else if (tagId == TAG_FILTER_TYPE)
        {
            //not supported
        }
        else if (tagId == TAG_MOVE)
        {
            //handle any existing command (to cater for nested commands)
            doParentCommand();
            
            //initialize the MOVE command
            inCommand = new Cmd(Cmd.CMD_MOVE);
        }
    }
    
    public void onTagEnd(int tagId) 
        throws WbxmlException
    {
        super.onTagEnd(tagId);

        //determine the ID of the parent and grandparent tags (if any)
        int parentId = getParentId();
        int grandParentId = getAncestorId(1);
        
        if ( (tagId == TAG_ATOMIC) || (tagId == TAG_SEQUENCE) || (tagId == TAG_SYNC) )
        {
            //handle the end of the parent command
            if (inNestedCmds.size() > 0)
                syncSession.onSyncCommandEnd( (Cmd)inNestedCmds.pop() );
        }
        else if (tagId == TAG_SYNC_HEADER)
        {
            //handle the sync header
            syncSession.onSyncHeader(inSyncHeader);
            inSyncHeader = null;
        }
        else if (tagId == TAG_ITEM)
        {
            //handle the item
            if (parentId == TAG_STATUS)
                inStatus.items.addElement(inItem);
            else if (isCommand(parentId))
                inCommand.items.addElement(inItem);
            inItem = null;
        }
        else if (tagId == TAG_MAPITEM)
        {
            //handle the map item
            if (parentId == TAG_MAP)
                inCommand.mapItems.addElement(inMapItem);
            inMapItem = null;
        }
        else if (tagId == TAG_META)
        {
            //handle the meta information
            Metinf metinf = cpMetinf.getMetinf();
            cpMetinf.clear();

            if (parentId == TAG_SYNC_HEADER)
            {
                inSyncHeader.metinf = metinf;
            }
            else if (parentId == TAG_CRED)
            {
                if (grandParentId == TAG_SYNC_HEADER)
                    inSyncHeader.credentials.metinf = metinf;
                else if (grandParentId == TAG_STATUS)
                    inStatus.credentials.metinf = metinf;
                else if (isCommand(grandParentId))
                    inCommand.credentials.metinf = metinf;
            }
            else if (parentId == TAG_CHAL)
            {
                if (grandParentId == TAG_STATUS)
                    inStatus.challenge.metinf = metinf;
            }
            else if (parentId == TAG_ITEM)
            {
                inItem.metinf = metinf;
            }
            else if (isCommand(parentId))
            {
                inCommand.metinf = metinf;
            }
        }
        else if (tagId == TAG_STATUS)
        {
            //handle the status command 
            syncSession.onSyncStatus(inStatus);
            inStatus = null;
        }
        else if (isCommand(tagId))
        {
            //handle the command
            syncSession.onSyncCommand(inCommand);
            inCommand = null;
        }
    }
    
    public void onStringData(int tagId, String data) 
        throws WbxmlException
    {
        super.onStringData(tagId, data);

        //determine the ID of the parent and grandparent tags (if any)
        int parentId = getParentId();
        int grandParentId = getAncestorId(1);
        
        if (tagId == TAG_CMD)
        {
            //set the command that the status refers to
            if (parentId == TAG_STATUS)
                inStatus.refCommand = data;
        }
        else if (tagId == TAG_CMD_ID)
        {
            //set the ID of the command
            int cmdId = parseInt(data, "CmdId");
            if (parentId == TAG_STATUS)
                inStatus.commandId = cmdId;
            else if (isCommand(parentId))
                inCommand.commandId = cmdId;
        }
        else if (tagId == TAG_CMD_REF)
        {
            //set the command ID that the status refers to
            int refCmdId = parseInt(data, "CmdRef");
            if (parentId == TAG_STATUS)
                inStatus.refCommandId = refCmdId;
            else if (parentId == TAG_RESULTS)
                inCommand.refCommandId = refCmdId;
        }
        else if (tagId == TAG_DATA)
        {
            //set the command data
            if (parentId == TAG_STATUS)
            {
                //data indicates the status code of the original command
                inStatus.statusCode = parseInt(data, "Data");
            }
            else if (parentId == TAG_ALERT)
            {
                //data indicates the alert code of a ALERT command
                inCommand.alertCode = parseInt(data, "Data");
            }
            else if (parentId == TAG_CRED)
            {
                //data indicates the credentials information
                byte[] dataBytes = parseBytes(data, "Data");
                if (grandParentId == TAG_SYNC_HEADER)
                    inSyncHeader.credentials.data = dataBytes;
                else if (grandParentId == TAG_STATUS)
                    inStatus.credentials.data = dataBytes;
                else if (isCommand(grandParentId))
                    inCommand.credentials.data = dataBytes;
            }
            else if (parentId == TAG_ITEM)
            {
                //data is the data of the item
                inItem.data = parseBytes(data, "Data");
            }
            else if (parentId == TAG_SEARCH)
            {
                //data is additional search data
                inCommand.data = parseBytes(data, "Data");
            }
        }
        else if (tagId == TAG_LANG)
        {
            //set the preferred language
            if ( (parentId == TAG_GET) || (parentId == TAG_PUT) || (parentId == TAG_SEARCH) )
                inCommand.language = data;
        }
        else if (tagId == TAG_LOC_NAME)
        {
            //set the source or target name
            if (parentId == TAG_SOURCE)
            {
                if (grandParentId == TAG_SYNC_HEADER)
                    inSyncHeader.sourceName = data; 
            }
            else if (parentId == TAG_TARGET)
            {
                if (grandParentId == TAG_SYNC_HEADER)
                    inSyncHeader.targetName = data;
            }
        }
        else if (tagId == TAG_LOC_URI)
        {
            //set the source or target URI
            if (parentId == TAG_SOURCE)
            {
                if (grandParentId == TAG_SYNC_HEADER)
                    inSyncHeader.sourceUri = data; 
                else if (grandParentId == TAG_ITEM)
                    inItem.sourceUri = data;
                else if (grandParentId == TAG_MAPITEM)
                    inMapItem.sourceUri = data;
                else if ( (grandParentId == TAG_MAP) || (grandParentId == TAG_SEARCH) || (grandParentId == TAG_SYNC) )
                    inCommand.sourceUri = data;
            }
            else if (parentId == TAG_TARGET)
            {
                if (grandParentId == TAG_SYNC_HEADER)
                    inSyncHeader.targetUri = data;
                else if (grandParentId == TAG_ITEM)
                    inItem.targetUri = data;
                else if (grandParentId == TAG_MAPITEM)
                    inMapItem.targetUri = data;
                else if ( (grandParentId == TAG_MAP) || (grandParentId == TAG_SEARCH) || (grandParentId == TAG_SYNC) )
                    inCommand.targetUri = data;
            }
            else if (parentId == TAG_SOURCE_PARENT)
            {
                if (grandParentId == TAG_ITEM)
                    inItem.sourceParentUri = data;
            }
            else if (parentId == TAG_TARGET_PARENT)
            {
                if (grandParentId == TAG_ITEM)
                    inItem.targetParentUri = data;
            }
        }
        else if (tagId == TAG_MSG_ID)
        {
            //set the message ID
            int msgId = parseInt(data, "MsgId");
            if (parentId == TAG_SYNC_HEADER)
                inSyncHeader.messageId = msgId;
        }
        else if (tagId == TAG_MSG_REF)
        {
            //set the ID of the message that the status refers to 
            if (parentId == TAG_STATUS)
                inStatus.refMessageId = parseInt(data, "MsgRef");
            else if (parentId == TAG_RESULTS)
                inCommand.refMessageId = parseInt(data, "MsgRef");
        }
        else if (tagId == TAG_RESP_URI)
        {
            //set the response URI
            if (parentId == TAG_SYNC_HEADER)
                inSyncHeader.responseUri = data;
        }
        else if (tagId == TAG_SESSION_ID)
        {
            //set the session ID
            if (parentId == TAG_SYNC_HEADER)
                inSyncHeader.sessionId = data;
        }
        else if (tagId == TAG_SOURCE_REF)
        {
            //add the source ID of an item that the status/results refers to 
            if (parentId == TAG_STATUS)
                inStatus.refItemSourceUris.addElement(data);
            else if (parentId == TAG_RESULTS)
                inCommand.refItemSourceUri = data;
        }
        else if (tagId == TAG_TARGET_REF)
        {
            //add the target ID of an item that the status/results refers to 
            if (parentId == TAG_STATUS)
                inStatus.refItemTargetUris.addElement(data);
            else if (parentId == TAG_RESULTS)
                inCommand.refItemTargetUri = data;
        }
        else if (tagId == TAG_RESERVED)
        {
            //ignore
        }
        else if (tagId == TAG_VER_DTD)
        {
            //set DTD version
            if (parentId == TAG_SYNC_HEADER)
                inSyncHeader.dtdVersion = data;
        }
        else if (tagId == TAG_VER_PROTO)
        {
            //set protocol version
            if (parentId == TAG_SYNC_HEADER)
                inSyncHeader.protocolVersion = data;
        }
        else if (tagId == TAG_NUMBER_OF_CHANGES)
        {
            //set the number of changes to expect in the sync
            if (parentId == TAG_SYNC)
                inCommand.numberOfChanges = parseInt(data, "NumberOfChanges");
        }
        else if (tagId == TAG_FILTER_TYPE)
        {
            //not supported
        }
    }
    
    public void onOpaqueDataBegin(int tagId, long length) 
        throws WbxmlException
    {
        super.onOpaqueDataBegin(tagId, length);

        //reset the stream used to store the data of the item
        inDataStream.reset();            
    }
    
    public void onOpaqueData(int tagId, byte[] data, int length) 
        throws WbxmlException
    {
        super.onOpaqueData(tagId, data, length);

        //store the data of the item
        inDataStream.write(data, 0, length);
    }

    public void onOpaqueDataEnd(int tagId, boolean commit) 
        throws WbxmlException
    {
        super.onOpaqueDataEnd(tagId, commit);

        if (commit)
        {
            //determine the ID of the parent tag (if any)
            int parentId = getParentId();
            int grandParentId = getAncestorId(1);
            
            if (tagId == TAG_DATA)
            {
                byte[] dataBytes = inDataStream.toByteArray();
                if (parentId == TAG_CRED)
                {
                    //data indicates the credentials information
                    if (grandParentId == TAG_SYNC_HEADER)
                        inSyncHeader.credentials.data = dataBytes;
                    else if (grandParentId == TAG_STATUS)
                        inStatus.credentials.data = dataBytes;
                    else if (isCommand(grandParentId))
                        inCommand.credentials.data = dataBytes;
                }
                else if (parentId == TAG_ITEM)
                {
                    //data is the data of the item
                    inItem.data = dataBytes;
                }
                else if (parentId == TAG_SEARCH)
                {
                    //data is additional search data
                    inCommand.data = dataBytes;
                }
            }
        }
    }
    
    
    /**
     * Writes the specified SyncML header in WBXML format to the specified output stream.
     * 
     * @param outputStream  the output stream to write the SyncML header to.
     * @param header        the header information to write.
     * @throws IOException      if the SyncML header couldn't be written.
     * @throws WbxmlException   if there was a WBXML formatting error.
     */
    public void writeHeader(OutputStream outputStream, SyncHdr header)
        throws WbxmlException, IOException
    {
        //determine which SyncML version to use
        String docId;
        if (header.dtdVersion.equals(VER_DTD_1_1))
            docId = DOC_ID_1_1;
        else if (header.dtdVersion.equals(VER_DTD_1_2))
            docId = DOC_ID_1_2;
        else
            throw new WbxmlException("invalid DTD version specified: " + header.dtdVersion);
        
        //write WBXML header
        Wbxml.writeHeader(outputStream, Wbxml.VERSION_1_2, Wbxml.CHARSET_UTF8, docId);
        
        writeTag(outputStream, TAG_SYNCML, true);

        writeTag(outputStream, TAG_SYNC_HEADER, true);

        writeTag(outputStream, TAG_VER_DTD, header.dtdVersion);
        writeTag(outputStream, TAG_VER_PROTO, header.protocolVersion);
        writeTag(outputStream, TAG_SESSION_ID, header.sessionId);
        writeTag(outputStream, TAG_MSG_ID, Integer.toString(header.messageId));

        writeTag(outputStream, TAG_SOURCE, true);
        writeTag(outputStream, TAG_LOC_URI, header.sourceUri);
        if ( (header.sourceName != null) && (header.sourceName.length() > 0) )
            writeTag(outputStream, TAG_LOC_NAME, header.sourceName);
        writeTagEnd(outputStream, TAG_SOURCE);

        writeTag(outputStream, TAG_TARGET, true);
        writeTag(outputStream, TAG_LOC_URI, header.targetUri);
        if ( (header.targetName != null) && (header.targetName.length() > 0) )
            writeTag(outputStream, TAG_LOC_NAME, header.targetName);
        writeTagEnd(outputStream, TAG_TARGET);
        
        if ( (header.responseUri != null) && (header.responseUri.length() > 0) )
            writeTag(outputStream, TAG_RESP_URI, header.responseUri);
        
        if (header.noResponse)
            writeTag(outputStream, TAG_NO_RESP, false);
        
        if (header.credentials != null)
            writeCred(outputStream, header.credentials);
        
        if (header.metinf != null)
            writeMetinf(outputStream, header.metinf);

        writeTagEnd(outputStream, TAG_SYNC_HEADER);

        //write the start of the sync body
        writeTag(outputStream, TAG_SYNC_BODY, true);
    }
    
    /**
     * Writes the SyncML footer in WBXML format to the specified output stream.
     * 
     * @param outputStream  the output stream to write the SyncML footer to.
     * @param finalMsg      indicates whether or not the message being written is the final message of the package.
     * @throws IOException      if the SyncML footer couldn't be written.
     * @throws WbxmlException   if there was a WBXML formatting error.
     */
    public void writeFooter(OutputStream outputStream, boolean finalMsg)
        throws WbxmlException, IOException
    {
        if (finalMsg)
            writeTag(outputStream, TAG_FINAL, false);

        writeTagEnd(outputStream, TAG_SYNC_BODY);

        writeTagEnd(outputStream, TAG_SYNCML);
    }
    
    /**
     * Writes the specified SyncML Status command in WBXML format to the specified output stream.
     * 
     * @param outputStream  the output stream to write the Status command to.
     * @param status        the Status command to write.
     * @throws IOException      if the Status command couldn't be written.
     * @throws WbxmlException   if there was a WBXML formatting error.
     */
    public void writeStatus(OutputStream outputStream, Status status)
        throws WbxmlException, IOException
    {
        writeTag(outputStream, TAG_STATUS, true);

        writeTag(outputStream, TAG_CMD_ID, Integer.toString(status.commandId));
        writeTag(outputStream, TAG_MSG_REF, Integer.toString(status.refMessageId));
        writeTag(outputStream, TAG_CMD_REF, Integer.toString(status.refCommandId));
        writeTag(outputStream, TAG_CMD, status.refCommand);
        
        if (status.refItemTargetUris != null)
        {
            for (int i = 0; i < status.refItemTargetUris.size(); i++)
            {
                String refItemTargetUri = (String)status.refItemTargetUris.elementAt(i);
                if ( (refItemTargetUri != null) && (refItemTargetUri.length() > 0) )
                    writeTag(outputStream, TAG_TARGET_REF, refItemTargetUri);
            }
        }
        
        if (status.refItemSourceUris != null)
        {
            for (int i = 0; i < status.refItemSourceUris.size(); i++)
            {
                String refItemSourceUri = (String)status.refItemSourceUris.elementAt(i);
                if ( (refItemSourceUri != null) && (refItemSourceUri.length() > 0) )
                    writeTag(outputStream, TAG_SOURCE_REF, refItemSourceUri);
            }
        }

        writeTag(outputStream, TAG_DATA, Integer.toString(status.statusCode));
        
        if (status.credentials != null)
            writeCred(outputStream, status.credentials);
        
        if (status.challenge != null)
            writeChal(outputStream, status.challenge);
        
        if (status.items != null)
        {
            for (int i = 0; i < status.items.size(); i++)
            {
                SyncItem item = (SyncItem)status.items.elementAt(i);
                if (item != null)
                    writeItem(outputStream, item);
            }
        }

        writeTagEnd(outputStream, TAG_STATUS);
    }
    
    /**
     * Writes the specified SyncML command in WBXML format to the specified output stream. <br/><br/>
     * 
     * In the case of commands that can contain child commands (Atomic, Sequence and Sync), 
     * {@link #writeCommandEnd(OutputStream, Cmd)} must be called when all child commands have
     * been written.
     * 
     * @param outputStream  the output stream to write the command to.
     * @param cmd           the command to write.
     * @throws IOException      if the command information couldn't be written.
     * @throws WbxmlException   if there was a WBXML formatting error.
     */
    public void writeCommand(OutputStream outputStream, Cmd cmd)
        throws WbxmlException, IOException
    {
        byte cmdTagId = 0;
        if (cmd.command.equals(Cmd.CMD_ADD))
            cmdTagId = TAG_ADD;
        else if (cmd.command.equals(Cmd.CMD_ALERT))
            cmdTagId = TAG_ALERT;
        else if (cmd.command.equals(Cmd.CMD_ATOMIC))
            cmdTagId = TAG_ATOMIC;
        else if (cmd.command.equals(Cmd.CMD_COPY))
            cmdTagId = TAG_COPY;
        else if (cmd.command.equals(Cmd.CMD_DELETE))
            cmdTagId = TAG_DELETE;
        else if (cmd.command.equals(Cmd.CMD_EXEC))
            cmdTagId = TAG_EXEC;
        else if (cmd.command.equals(Cmd.CMD_GET))
            cmdTagId = TAG_GET;
        else if (cmd.command.equals(Cmd.CMD_MAP))
            cmdTagId = TAG_MAP;
        else if (cmd.command.equals(Cmd.CMD_MOVE))
            cmdTagId = TAG_MOVE;
        else if (cmd.command.equals(Cmd.CMD_PUT))
            cmdTagId = TAG_PUT;
        else if (cmd.command.equals(Cmd.CMD_REPLACE))
            cmdTagId = TAG_REPLACE;
        else if (cmd.command.equals(Cmd.CMD_RESULTS))
            cmdTagId = TAG_RESULTS;
        else if (cmd.command.equals(Cmd.CMD_SEARCH))
            cmdTagId = TAG_SEARCH;
        else if (cmd.command.equals(Cmd.CMD_SEQUENCE))
            cmdTagId = TAG_SEQUENCE;
        else if (cmd.command.equals(Cmd.CMD_SYNC))
            cmdTagId = TAG_SYNC;
        else
            throw new WbxmlException("invalid command '" + cmd.command + "' specified");

        writeTag(outputStream, cmdTagId, true);
        writeTag(outputStream, TAG_CMD_ID, Integer.toString(cmd.commandId));
        
        if ( (cmd.sourceUri != null) && (cmd.sourceUri.length() > 0) )
        {
            writeTag(outputStream, TAG_SOURCE, true);
            writeTag(outputStream, TAG_LOC_URI, cmd.sourceUri);
            writeTagEnd(outputStream, TAG_SOURCE);
        }

        if ( (cmd.targetUri != null) && (cmd.targetUri.length() > 0) )
        {
            writeTag(outputStream, TAG_TARGET, true);
            writeTag(outputStream, TAG_LOC_URI, cmd.targetUri);
            writeTagEnd(outputStream, TAG_TARGET);
        }
        
        if (cmd.noResponse)
            writeTag(outputStream, TAG_NO_RESP, false);
        
        if (cmd.credentials != null)
            writeCred(outputStream, cmd.credentials);
        
        if (cmd.metinf != null)
            writeMetinf(outputStream, cmd.metinf);
        
        if ( (cmd.language != null) && (cmd.language.length() > 0) )
            writeTag(outputStream, TAG_LANG, cmd.language);
        
        if (cmd.items != null)
        {
            for (int i = 0; i < cmd.items.size(); i++)
            {
                SyncItem item = (SyncItem)cmd.items.elementAt(i);
                if (item != null)
                    writeItem(outputStream, item);
            }
        }

        if (cmd.command.equals(Cmd.CMD_ALERT))
        {
            if (cmd.alertCode >= 0)
                writeTag(outputStream, TAG_DATA, Integer.toString(cmd.alertCode));
        }        
        
        if (cmd.command.equals(Cmd.CMD_DELETE))
        {
            if (cmd.archivedDelete)
                writeTag(outputStream, TAG_ARCHIVE, false);

            if (cmd.softDelete)
                writeTag(outputStream, TAG_SOFT_DEL, false);
        }

        if (cmd.command.equals(Cmd.CMD_MAP))
        {
            if (cmd.mapItems != null)
            {
                for (int i = 0; i < cmd.mapItems.size(); i++)
                {
                    MapItem mapItem = (MapItem)cmd.mapItems.elementAt(i);
                    if (mapItem != null)
                        writeMapItem(outputStream, mapItem);
                }
            }
        }

        if (cmd.command.equals(Cmd.CMD_SYNC))
        {
            if (cmd.numberOfChanges >= 0)
                writeTag(outputStream, TAG_NUMBER_OF_CHANGES, Integer.toString(cmd.numberOfChanges));
        }
        
        if ( (cmd.command.equals(Cmd.CMD_SEARCH)) || (cmd.command.equals(Cmd.CMD_RESULTS)) )
        {
            if (cmd.noResults)
                writeTag(outputStream, TAG_NO_RESULTS, false);
        }
        
        if (cmd.command.equals(Cmd.CMD_RESULTS))
        {
            if (cmd.refCommandId >= 0)
                writeTag(outputStream, TAG_CMD_REF, Integer.toString(cmd.refCommandId));

            if (cmd.refMessageId >= 0)
                writeTag(outputStream, TAG_MSG_REF, Integer.toString(cmd.refMessageId));
            
            if ( (cmd.refItemSourceUri != null) && (cmd.refItemSourceUri.length() > 0) )
                writeTag(outputStream, TAG_SOURCE_REF, cmd.refItemSourceUri);
            
            if ( (cmd.refItemTargetUri != null) && (cmd.refItemTargetUri.length() > 0) )
                writeTag(outputStream, TAG_TARGET_REF, cmd.refItemTargetUri);
        }
        
        if (cmd.command.equals(Cmd.CMD_SEARCH))
        {
            if ( (cmd.data != null) && (cmd.data.length > 0) )
                writeTag(outputStream, TAG_DATA, cmd.data);
        }
        
        //only write the end tag if the command can't contain child commands
        if ( (! cmd.command.equals(Cmd.CMD_ATOMIC)) && (! cmd.command.equals(Cmd.CMD_SEQUENCE)) && (! cmd.command.equals(Cmd.CMD_SYNC)) )
            writeTagEnd(outputStream, cmdTagId);
    }
    
    /**
     * Writes the end of the specified SyncML command in WBXML format to the specified output stream. <br/><br/>
     * 
     * This method only applies to commands that can contain child commands (Atomic, Sequence and Sync).
     * 
     * @param outputStream  the output stream to write the command end to.
     * @param cmd           the command end to write.
     * @throws IOException      if the command end couldn't be written.
     * @throws WbxmlException   if there was a WBXML formatting error.
     */
    public void writeCommandEnd(OutputStream outputStream, Cmd cmd)
        throws WbxmlException, IOException
    {
        //only write the end tag if the command can contain child commands - otherwise, the end tag has already been written
        if (cmd.command.equals(Cmd.CMD_ATOMIC))
            writeTagEnd(outputStream, TAG_ATOMIC);
        else if (cmd.command.equals(Cmd.CMD_SEQUENCE))
            writeTagEnd(outputStream, TAG_SEQUENCE);
        if (cmd.command.equals(Cmd.CMD_SYNC))
            writeTagEnd(outputStream, TAG_SYNC);
    }
    
    /* Writes the specified SyncItem to the specified output stream. */
    private void writeItem(OutputStream outputStream, SyncItem item)
        throws WbxmlException, IOException
    {
        writeTag(outputStream, TAG_ITEM, true);
        
        if ( (item.sourceUri != null) && (item.sourceUri.length() > 0) )
        {
            writeTag(outputStream, TAG_SOURCE, true);
            writeTag(outputStream, TAG_LOC_URI, item.sourceUri);
            writeTagEnd(outputStream, TAG_SOURCE);
        }

        if ( (item.targetUri != null) && (item.targetUri.length() > 0) )
        {
            writeTag(outputStream, TAG_TARGET, true);
            writeTag(outputStream, TAG_LOC_URI, item.targetUri);
            writeTagEnd(outputStream, TAG_TARGET);
        }

        if ( (item.sourceParentUri != null) && (item.sourceParentUri.length() > 0) )
        {
            writeTag(outputStream, TAG_SOURCE_PARENT, true);
            writeTag(outputStream, TAG_LOC_URI, item.sourceParentUri);
            writeTagEnd(outputStream, TAG_SOURCE_PARENT);
        }
        
        if ( (item.targetParentUri != null) && (item.targetParentUri.length() > 0) )
        {
            writeTag(outputStream, TAG_TARGET_PARENT, true);
            writeTag(outputStream, TAG_LOC_URI, item.targetParentUri);
            writeTagEnd(outputStream, TAG_TARGET_PARENT);
        }
        
        if (item.metinf != null)
            writeMetinf(outputStream, item.metinf);

        if ( (item.data != null) && (item.data.length > 0) )
            writeTag(outputStream, TAG_DATA, item.data);
        
        if (item.moreData)
            writeTag(outputStream, TAG_MORE_DATA, false);

        writeTagEnd(outputStream, TAG_ITEM);
    }
    
    /* Writes the specified MapItem to the specified output stream. */
    private void writeMapItem(OutputStream outputStream, MapItem mapItem)
        throws WbxmlException, IOException
    {
        writeTag(outputStream, TAG_MAPITEM, true);
        
        if ( (mapItem.sourceUri != null) && (mapItem.sourceUri.length() > 0) )
        {
            writeTag(outputStream, TAG_SOURCE, true);
            writeTag(outputStream, TAG_LOC_URI, mapItem.sourceUri);
            writeTagEnd(outputStream, TAG_SOURCE);
        }

        if ( (mapItem.targetUri != null) && (mapItem.targetUri.length() > 0) )
        {
            writeTag(outputStream, TAG_TARGET, true);
            writeTag(outputStream, TAG_LOC_URI, mapItem.targetUri);
            writeTagEnd(outputStream, TAG_TARGET);
        }

        writeTagEnd(outputStream, TAG_MAPITEM);
    }
    
    /* Writes the specified credentials to the specified output stream. */
    private void writeCred(OutputStream outputStream, Cred cred)
        throws WbxmlException, IOException
    {
        writeTag(outputStream, TAG_CRED, true);
        
        if (cred.metinf != null)
            writeMetinf(outputStream, cred.metinf);

        if ( (cred.data != null) && (cred.data.length > 0) )
            writeTag(outputStream, TAG_DATA, cred.data);

        writeTagEnd(outputStream, TAG_CRED);
    }
    
    /* Writes the specified challenge to the specified output stream. */
    private void writeChal(OutputStream outputStream, Chal chal)
        throws WbxmlException, IOException
    {
        writeTag(outputStream, TAG_CHAL, true);
        
        if (chal.metinf != null)
            writeMetinf(outputStream, chal.metinf);

        writeTagEnd(outputStream, TAG_CHAL);
    }
    
    /* Writes the specified Meta information to the specified output stream. */
    private void writeMetinf(OutputStream outputStream, Metinf metinf)
        throws WbxmlException, IOException
    {
        writeTag(outputStream, TAG_META, true);
        
        Wbxml.writePageSwitch(outputStream, MetInfCodepage.PAGE_INDEX);
        cpMetinf.setNestingLevel(getNestingLevel());
        cpMetinf.writeMetinf(outputStream, metinf);
        Wbxml.writePageSwitch(outputStream, PAGE_INDEX);
        writeTagEnd(outputStream, TAG_META);
    }

    
    /* Parses the specified data as an Integer. */
    private int parseInt(String data, String name)
        throws WbxmlException
    {
        try
        {
            return Integer.parseInt(data);
        }
        catch (NumberFormatException e)
        {
            throw new WbxmlException("invalid '" + name + "' value received from server: " + data, e);
        }
    }
    
    /* Parses the specified data as an array of bytes. */
    private byte[] parseBytes(String data, String name)
        throws WbxmlException
    {
        try
        {
            return data.getBytes(ENCODING_UTF8);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new WbxmlException("invalid UTF-8 '" + name + "' value received from server: " + data, e);
        }
    }
    
    /* Returns TRUE if the specified tag represents a SyncML command. */
    private boolean isCommand(int tagId)
    {
        return ( (tagId == TAG_ADD) || (tagId == TAG_ALERT) || (tagId == TAG_ATOMIC) || (tagId == TAG_COPY) || (tagId == TAG_DELETE) || (tagId == TAG_EXEC) || (tagId == TAG_GET) || (tagId == TAG_MAP) || (tagId == TAG_PUT) || (tagId == TAG_REPLACE) || (tagId == TAG_RESULTS) || (tagId == TAG_SEARCH) || (tagId == TAG_SEQUENCE) || (tagId == TAG_SYNC) || (tagId == TAG_MOVE) );
    }
    
    /* Processes nested command. */
    private void doParentCommand()
        throws WbxmlException
    {
        //nothing more to do if there is no command
        if (inCommand == null)
            return;
        
        //make sure the current command is one that can contain child commands
        if ( (! inCommand.command.equals(Cmd.CMD_ATOMIC)) && (! inCommand.command.equals(Cmd.CMD_SEQUENCE)) && (! inCommand.command.equals(Cmd.CMD_SYNC)) )
            throw new WbxmlException("invalid command nesting - parent command must be either Atomic, Sequence or Sync");
        
        //handle the parent command now
        syncSession.onSyncCommand(inCommand);
        inCommand = null;
    }
}
