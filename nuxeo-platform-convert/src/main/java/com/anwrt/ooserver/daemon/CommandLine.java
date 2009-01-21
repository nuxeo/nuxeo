package com.anwrt.ooserver.daemon;

import java.util.HashMap;

import com.sun.star.beans.NamedValue;
import com.sun.star.bridge.XUnoUrlResolver;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.container.XNameAccess;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

/**
 * All command line specific menus and informations are made here
 * <br>
 * creation : 24 ao�t 07
 *
 * @author <a href="mailto:oodaemon@extraserv.net">Jounayd Id Salah</a>
 */
public final class CommandLine
{
    /** Not instance allowed, purely static */
    private CommandLine() { /*DO NOTHING*/ }

    /** displayed tag for command line specific errors */
    public static final String ERROR_TAG = "error : ";

    private static Config _config = null;

    private static final String	OPTION_PREFIX			= "-";
    private static final String SPECIAL_OPTION_PREFIX 	= "--";
    private static final int 	_minArgs				= 2;
    private static final String	_optConfig				= "config";
    private static final int	_optConfigArgPos 		= 1;
    private static final int 	_optConfigShift 		= 2;
    private static final String	_optAdmin				= "admin";
    private static final int 	_optAdminArgPos			= 1;
    private static final int 	_optAdminShift 			= 2;
    private static final String	_optLogger				= "logger";
    private static final int 	_optLoggerArgPos		= 1;
    private static final int 	_optLoggerShift			= 2;
    private static final String	_optVersion				= "version";
    private static final String	_optHelp				= "help";
    public static final String	cmdStatus				= "status";
    public static final String 	cmdStop					= "stop";

    /**
     * message displayed when starting the daemon
     * @param isAdmin displays a special message if the daemon is launched as admin
     * @return a string containing the message (contains 'new line' characters)
     */
    private static String getDaemonStartupMessage(boolean isAdmin)
    {
        String line = "kkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkk\n\n";
        String admin = "";
        if (isAdmin)
            admin = " # ADMIN MODE #";
        String msg =
            "\n" + line
            + "\t\t\tOPEN OFFICE DAEMON v1.0 beta" + admin + "\n" + "\n"
            + line;

        return msg;
    }
    /**
     * Change log level.<br>
     * If failed, displays informations about levels
     * @param logLevel string corresponding to a log level (DEBUG, DETAILED_DEBUG, etc...)
     */
    private static void setLogLevel(String logLevel)
    {
        if (logLevel != null)
        {
            try
            {
                Logger.setLevel(logLevel);
            }
            catch (IncorrectLoggerLevelException ex)
            {
                System.err.println(ERROR_TAG + ex);
                System.out.println("  LEVELS : ");
                System.out.println(Logger.levelInformation("  ") + "\n");
            }
        }
    }
    /**
     * Checks if the admin command is allowed
     * @param command a string representing the command (ex : stop)
     * @return true if the command is valid
     */
    private static boolean isAdminCommandValid(String command)
    {
        if (command.equals(cmdStatus)) return true;
        if (command.equals(cmdStop)) return true;
        return false;
    }
    /**
     * returns the command url (which is supposed to be sent to an open office
     * server instance).
     * @param command a command (ex : stop)
     * @return the uno url
     */
    private static String getCommandUrl(String command)
    {
        return "uno:" + _config.adminAcceptor + ";urp" + ";daemon." + command;
    }
    /**
     * Print status to output buffer
     * @param status status interface for getting informations from the daemon instance
     * @throws Exception
     */
    private static void printStatus(XNameAccess status) throws Exception
    {
        System.out.println("STATUS");
        System.out.println("Instances in daemon (free/total) : "
                + status.getByName("available") + "/"
                + status.getByName("poolsize"));

        Object[] workers = (Object[]) status.getByName("workers");
        System.out.println("Worker\tId\tin use\tusages\tduration\tuser-directory");

        for (int i = 0; i < workers.length; i++)
        {
            HashMap worker 		= namedValueArrayToHashMap((NamedValue[]) workers[i]);
            String out 			= "";
            String inuse 		= " ";
            String duration 	= "        \t";
            Long usageTime = (Long) worker.get("usage-time");
            if (usageTime.longValue() > (long) 0)
            {
                inuse = "x";
                duration = (Math.round(usageTime.longValue() * 0.001)) + "s    \t";
            }
            System.out.println("Worker\t" + worker.get("index") + "\t"
                    + inuse + "\t"
                    + worker.get("usage") + "\t"
                    + duration
                    + worker.get("user-dir"));
        }
    }
    private static HashMap namedValueArrayToHashMap(NamedValue[] array)
    {
        HashMap ret = new HashMap();
        for (int i = 0; i < array.length; i++)
            ret.put(array[i].Name, array[i].Value);
        return ret;
    }
    /**
     * Executes command and ask directly to the daemon instance running if necessary using
     * some connection (socket, etc...)
     * @param command command to execute (ex : stop)
     * @throws Exception any error that could occur
     */
    private static void executeCommand(String command) throws Exception
    {
        if (!isAdminCommandValid(command)) throw new Exception("invalid command " + command);
        XComponentContext initialContext = Bootstrap.createInitialComponentContext(null);
        XMultiComponentFactory manager = initialContext.getServiceManager();
        Object unoResolverObj = manager.createInstanceWithContext("com.sun.star.bridge.UnoUrlResolver", initialContext);
        XUnoUrlResolver unoUrlResolver = (XUnoUrlResolver) UnoRuntime.queryInterface(XUnoUrlResolver.class, unoResolverObj);

        String url = getCommandUrl(command);
        try
        {
            if (command.equals(cmdStatus))
            {
                Object statusObj = unoUrlResolver.resolve(url);
                if (statusObj == null) throw new Exception("cannot resolve Status");
                XNameAccess status = (XNameAccess) UnoRuntime.queryInterface(XNameAccess.class, statusObj);
                printStatus(status);
            }
            else if (command.equals(cmdStop))
            {
                unoUrlResolver.resolve(url);
            }
        }
        catch (Exception ex)
        {
            Logger.debug("URL : " + url);
            throw ex;
        }
    }
    private static void startAdminMode(String configPath, String adminCommand, String logLevel)
    {
        if (!isAdminCommandValid(adminCommand))
        {
            Logger.error(ERROR_TAG + "invalid admin command : " + adminCommand);
        }
        try
        {
            ConfigHandler configHandler = new ConfigHandler();
            _config = configHandler.readConfiguration(configPath);
        }
        catch (Exception ex)
        {
            Logger.fatalError("Cannot read configuration", ex);
        }
        if (logLevel != null)
            setLogLevel(logLevel);
        try
        {
            executeCommand(adminCommand);
        }
        catch (Exception ex)
        {
            Logger.error("cannot execute command : " + adminCommand);
            Logger.debug(ex);
        }
    }
    private static void startNormalMode(String configPath, String logLevel)
    {
        Daemon daemon = new Daemon(configPath);

        setLogLevel(logLevel);
        daemon.run();
    }

    /**
     * Used to print informations about the -admin option argument (admin commands)
     */
    private static void printAdminCommands()
    {
        System.out.println(   " ADMIN_COMMAND : \n"
                + "   stop   : stops the current daemon. \n"
                + "   status : get information about the daemon. \n");
    }
    /**
     * Print daemon's version and other related informations to the output buffer
     */
    private static void printVersion()
    {
        System.out.println("\n\nOpen Office Server Daemon " + Daemon.getVersionString());
        System.out.println("Copyright (C) 2007 Anyware Technologies");
        System.out.println("License LGPLv3+ : GNU LGPL version 3 or later <http://gnu.org/licenses/lgpl.html>");
        System.out.println("This is free software : you are free to change and redistribute it.");
        System.out.println("There is NO WARRANTY, to the extent permitted by law.\n");
    }
    /**
     * Print daemon's help informations to the output buffer
     */
    private static void printHelpInformations()
    {
        System.out.println("\ncommand : Daemon " + OPTION_PREFIX + _optConfig
                + " <CONFIGURATION_FILE_PATH> [" + OPTION_PREFIX + _optAdmin + " ADMIN_COMMAND] ["
                + OPTION_PREFIX + _optLogger + " <LOG_LEVEL>] \n");
        System.out.println("          Daemon --version");
        System.out.println("          Daemon --help");
        System.out.println("\n");
        System.out.println(	  " CONFIGURATION_FILE_PATH :\n"
                            + "   An xml configuration file required. \n"
                            + "   An example could be found in <DAEMON_DIR>/config/ \n");
        printAdminCommands();
        System.out.println(   " LOG_LEVEL : \n"
                            + "   Specify a new log level \n"
                            + "   (higher priority than the log level in config file). \n");

        System.out.println(Logger.levelInformation("   "));
    }
    /**
     * Starts the command line menu, creates the daemon and runs it
     * @param args arguments that come from Daemon's main method
     */
    public static void start(String[] args)
    {
        boolean 	isAdmin 		= false;
        String 		configPath 		= null;
        String 		logLevel 		= null;
        int 		argPos 			= 0;
        String 		adminCommand 	= null;

        Logger.newInstance(new Log4JLogger());
        try
        {
            if (args.length < _minArgs)
            {
                try
                {
                    // Treat GNU recommended special options
                    if (args.length != 1) throw new Exception();
                    if (args[0].equals(SPECIAL_OPTION_PREFIX + _optVersion))
                    {
                        printVersion();
                        return;
                    }
                    else if (args[0].equals(SPECIAL_OPTION_PREFIX + _optHelp))
                    {
                        printHelpInformations();
                        return;
                    }
                    else throw new Exception();
                }
                catch (Exception ex)
                {
                    throw new Exception("wrong argument number");
                }
            }
            if (!args[argPos].equals(OPTION_PREFIX + _optConfig)) throw new Exception(OPTION_PREFIX + _optConfig + " argument required");
            configPath 	= args[argPos + _optConfigArgPos];
            argPos 		+= _optConfigShift;
            if (args.length >= argPos + 1)
            {
                if (args[argPos].equals(OPTION_PREFIX + _optAdmin))
                {
                    isAdmin = true;
                    if (args.length < argPos + _optAdminShift) throw new Exception("ADMIN_COMMAND not specified after optin " + OPTION_PREFIX + "admin");
                    adminCommand = args[argPos + _optAdminArgPos];
                    argPos += _optAdminShift;
                    if ((args.length >= argPos + 1) && args[argPos].equals(OPTION_PREFIX + _optLogger))
                    {
                        if (args.length < argPos + _optLoggerShift) throw new Exception("LOG_LEVEL not specified after option " + OPTION_PREFIX + "logger");
                        logLevel = args[argPos + _optLoggerArgPos];
                    }
                }
                else if (args[argPos].equals(OPTION_PREFIX + _optLogger))
                {
                    if (args.length < argPos + _optLoggerShift) throw new Exception("LOG_LEVEL not specified after option " + OPTION_PREFIX + "logger");
                    logLevel = args[argPos + _optLoggerArgPos];
                }
            }
        }
        catch (Exception ex)
        {
            //System.err.println(ERROR_TAG + ex.getMessage());
            Logger.error(ex.getMessage());
            printHelpInformations();

        }
        System.out.println("Configuration file path : " + configPath);
        System.out.println(getDaemonStartupMessage(isAdmin));

        // Choose which mode to start
        if (isAdmin)
            startAdminMode(configPath, adminCommand, logLevel);
        else
            startNormalMode(configPath, logLevel);

    }
}
