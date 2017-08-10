/**
 * Copyright © 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.test;


/**
 * A class providing for the parsing of command line arguments. <br/><br/>
 *
 * All arguments must be named. For example:
 * <pre>
 *      -f                  =>  option 'f' is set
 *      -f foo -b bar       =>  'f' = 'foo', 'b' = 'bar'
 *      -f "foo bar" -b bar =>  'f' = 'foo bar', 'b' = 'bar'
 * </pre>
 *
 * @author  Denis Evoy
 */
public class CmdLineParser
{
    private String[] cmdArgs = null;                    //the arguments to parse


    /** 
     * Constructs a command line parser to parse the specified command line. 
     *
     * @param cmdLine the command line to parse. Must not be null or empty.
     */
    public CmdLineParser(String cmdLine)
    {
        super();

        if ( (cmdLine == null) || (cmdLine.length() <= 0) )
            throw new IllegalArgumentException("no command line specified");

        cmdArgs = cmdLine.split(" \t");
        if ( (cmdArgs == null) || (cmdArgs.length <= 0) )
            throw new IllegalArgumentException("no arguments specified");
    }

    /** 
     * Constructs a command line parser to parse the specified arguments. 
     *
     * @param args the command line arguments to parse. Must not be null or empty.
     */
    public CmdLineParser(String[] args)
    {
        super();

        if (args == null)
            throw new IllegalArgumentException("no arguments specified");

        cmdArgs = args;
    }


    /** 
     * Returns the name of the command or null if one isn't present.
     * 
     * @return The name of the command. May be null or empty.
     */
    public String getCommandName()
    {
        if ( (cmdArgs.length > 0) && (! cmdArgs[0].startsWith("-")) )
            return cmdArgs[0];

        return null;
    }

    /** 
     * Returns the value of the specified argument within the command line or null if it isn't present. 
     *
     * @param name the name of the argument to retrieve. Must not be null or empty.
     * @return The value of the argument or null if it isn't present. May be null or empty.
     */
    public String getArgument(String name)
    {
        if ( (name == null) || (name.length() <= 0) )
            throw new IllegalArgumentException("no argument name specified");

        for (int i = 0; i < cmdArgs.length; i++)
        {
            if (cmdArgs[i].equals("-" + name))
                return (i + 1 < cmdArgs.length) ? cmdArgs[i + 1] : null;
        }

        return null;
    }

    /** 
     * Returns whether or not the specified option is set.
     *
     * @param name the name of the option to look for. Must not be null or empty.
     * @return <code>true</code> if the specified option is present.
     */
    public boolean isOption(String name)
    {
        if ( (name == null) || (name.length() <= 0) )
            throw new IllegalArgumentException("no option name specified");

        for (int i = 0; i < cmdArgs.length; i++)
        {
            if (cmdArgs[i].equals("-" + name))
                return true;
        }

        return false;
    }
}
