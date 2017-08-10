/**
 * Copyright © 2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.mtk.j2me.tools.discovery;


import java.util.Vector;

import javax.microedition.lcdui.*;

import net.cp.mtk.j2me.tools.discovery.basic.CharacterSets;
import net.cp.mtk.j2me.tools.discovery.basic.Midlet;
import net.cp.mtk.j2me.tools.discovery.basic.RMS;
import net.cp.mtk.j2me.tools.discovery.basic.SystemMemory;
import net.cp.mtk.j2me.tools.discovery.basic.SystemProperties;
import net.cp.mtk.j2me.tools.discovery.contacts.Contacts;
import net.cp.mtk.j2me.tools.discovery.files.FileSystem;
import net.cp.mtk.j2me.tools.discovery.ui.Lcdui;


public class TestRunForm extends Form implements Runnable, CommandListener
{
    private DiscoveryMIDlet midlet;

    private Thread testerThread;

    private StringItem testStatusItem;
    private Command viewCommand;
    private Command exitCommand;


    public TestRunForm(DiscoveryMIDlet midlet, String outputUrl)
    {
        super("Status");
        this.midlet = midlet;
        
        testerThread = null;
        
        testStatusItem = null;
        viewCommand = null;
        exitCommand = null;
        
        //set the output URL
        Logger.setOutputUrl(outputUrl);

        addContent();
        
        //run the tests in a separate thread
        testerThread = new Thread(this);
        testerThread.start();
    }

    private void addContent()
    {
        deleteAll();
        
        //show where we're logging to
        String logFileUrl = Logger.getOutputUrl();
        if ( (logFileUrl != null) && (logFileUrl.length() > 0) )
            DiscoveryMIDlet.addText(this, "Logging to: ", logFileUrl);
        else
            DiscoveryMIDlet.addText(this, "Logging to: ", "Memory");

        //show test status
        DiscoveryMIDlet.addEmptyLine(this);
        testStatusItem = DiscoveryMIDlet.addText(this, "Status: ", "Unknown");
        
        //add commands
        exitCommand = new Command("Exit", Command.EXIT, 1);
        addCommand(exitCommand);
        setCommandListener(this);
    }
    
    public void setTestStatus(String status)
    {
        if (testStatusItem != null)
            testStatusItem.setText(status);
    }

    private void showIssues()
    {
        Vector criticalIssues = Logger.getIssues(Logger.SEVERITY_CRITICAL);
        Vector highIssues = Logger.getIssues(Logger.SEVERITY_HIGH);
        Vector mediumIssues = Logger.getIssues(Logger.SEVERITY_MEDIUM);
        Vector lowIssues = Logger.getIssues(Logger.SEVERITY_LOW);
        
        DiscoveryMIDlet.addEmptyLine(this);
        DiscoveryMIDlet.addText(this, "Issues: ", null);
        DiscoveryMIDlet.addText(this, "    Critical: ", Integer.toString(criticalIssues.size()));
        DiscoveryMIDlet.addText(this, "    High: ", Integer.toString(highIssues.size()));
        DiscoveryMIDlet.addText(this, "    Medium: ", Integer.toString(mediumIssues.size()));
        DiscoveryMIDlet.addText(this, "    Low: ", Integer.toString(lowIssues.size()));
        
        DiscoveryMIDlet.addEmptyLine(this);
        String logFileUrl = Logger.getOutputUrl();
        if ( (logFileUrl != null) && (logFileUrl.length() > 0) )
        {
            DiscoveryMIDlet.addText(this, null, "See log file for the complete list of issues.");
        }
        else
        {
            viewCommand = new Command("View", Command.SCREEN, 2);
            addCommand(viewCommand);
            DiscoveryMIDlet.addText(this, null, "Select \"View\" for the complete list of issues.");
        }
    }

    public void commandAction(Command command, Displayable d)
    {
        if (command == exitCommand)
        {
            midlet.quitApp();
        }
        else if (command == viewCommand)
        {
            deleteAll();
            removeCommand(viewCommand);
            String logText = Logger.getOutputBuffer();
            if ( (logText != null) && (logText.length() > 0) ) 
                append(logText);
        }
    }
    
    public void run()
    {
        //run some tests
        setTestStatus("Running...");
        Midlet.evaluate(midlet);
        SystemMemory.evaluate(midlet);
        CharacterSets.evaluate(midlet);
        SystemProperties.evaluate(midlet);
        Lcdui.evaluate(midlet);
        RMS.evaluate(midlet);
        FileSystem.evaluate(midlet);
        Contacts.evaluate(midlet);
        setTestStatus("Complete");

        //write the list of issues to the log
        Logger.log("");
        Logger.log("-----------------------------------");
        Logger.log("ISSUES FOUND:");
        Logger.logIssues();
        Logger.log("-----------------------------------");
        
        //show a summary of the issues found
        showIssues();
    }
}
