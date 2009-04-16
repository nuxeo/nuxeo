package com.anwrt.ooserver.daemon;

import java.util.Iterator;
import java.util.Timer;
import com.sun.star.bridge.XBridgeFactory;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.connection.AlreadyAcceptingException;
import com.sun.star.connection.ConnectionSetupException;
import com.sun.star.connection.XAcceptor;
import com.sun.star.connection.XConnection;
import com.sun.star.connection.XConnectionBroadcaster;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

/**
 * Main Open Office daemon class.<br>
 * There are two entry points :<br>
 *  - you can run it from java using constructors to have one instance<br>
 *  - or using command line (see CommandLine for more informations)<br>
 *
 *  <strong>warning</strong> if there is no output, do not forget to instantiate the logger,
 *  (ex : Logger.newInstance(new LoggerBasicImpl());) otherwise the logger will not be enabled.<br>
 * <br>
 * creation : 24 ao�t 07
 *
 * @author <a href="mailto:oodaemon@extraserv.net">Jounayd Id Salah</a>
 * @see com.anwrt.ooserver.daemon.CommandLine
 */
public class Daemon
{
    public final static float 	VERSION			= 1.0f;
    public final static String 	VERSION_TYPE 	= "beta";

    private XComponentContext				_initialContext		= null;
    private Config							_config 			= null;
    private ProcessPool						_processPool		= null;
    private Timer							_shutdownThread 	= null;
    private XAcceptor						_acceptor			= null;
    private XBridgeFactory					_bridgeFactory		= null;
    private AdminAcceptorThread				_adminThread		= null;
    /** used to know when the daemon called the shutdown method
     * (basically to avoid unnecessary exceptions)	*/
    private boolean							_isShutdowned		= false;

    /**
     * returns current version
     * @return "x.x type" (ex : "1.0 beta")
     */
    public static String getVersionString()
    {
        return VERSION + " " + VERSION_TYPE;
    }
    /**
     * Get the Open Office server instance manager
     * @return ProcessPool class that manage server instances
     */
    public ProcessPool			getPool()				{ return _processPool; }
    public XComponentContext	getInitialContext()		{ return _initialContext; }
    public XBridgeFactory		getBridgeFactory()		{ return _bridgeFactory; }
    /**
     * get daemon current configuration
     * @return daemon configuration
     */
    public Config				getConfig()				{ return _config; }

    /** used to know when the daemon called the shutdown method
     * (basically to avoid unnecessary exceptions)
     * @return true if the daemon is in a shutdown process
     * (shutdown method just called and finished)*/
    public synchronized boolean isShutdowned()
    {
        return _isShutdowned;
    }

    /**
     * Creates a daemon and initialize it.
     * Then the daemon is waiting to be run.
     * @param config daemon's configuration
     */
    public Daemon(Config config)
    {
        _config = config;
        init();
    }
    /**
     * same as Daemon(Config), but the configuration will be read from an xml file;
     * @param configPath an xml file describing the configuration
     */
    public Daemon(String configPath)
    {
        try
        {
            ConfigHandler configHandler = new ConfigHandler();
            System.out.println("read configuration");
            _config = configHandler.readConfiguration(configPath);
            System.out.println("read configuration ok");
        }
        catch (Exception ex)
        {
            Logger.fatalError("Cannot read configuration", ex);
            return;
        }

        init();
    }

    /**
     * Inits configuration and Open Office API
     */
    private void init()
    {
        try
        {
            Logger.info("init starting ...");
            initConfig();
            initOOAPI();
            Logger.info("init OK");
        }
        catch (Exception ex)
        {
            Logger.fatalError("Cannot init OpenOffice daemon", ex);
        }
    }
    /**
     * Try to validate the configuration
     * @throws Exception cannot validate configuration, error will be specified with the message
     */
    private void initConfig() throws Exception
    {
        if (_config == null) throw new Exception("no configuration specified");
        _config.validate();
        Logger.info("\n" + _config);
    }

    /**
     * Init and get useful stuff from OpenOffice API
     * @throws Exception cannot init OpenOffice API, daemon won't run
     */
    private void initOOAPI() throws Exception
    {
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
        // Initial context
        _initialContext 			= Bootstrap.createInitialComponentContext(null);

        // Service Manager
        XMultiComponentFactory manager = _initialContext.getServiceManager();

        _processPool 		= new ProcessPool(_config);
        _shutdownThread 		= null;

        // Acceptor
        Object acceptorObj 	= manager
            .createInstanceWithContext("com.sun.star.connection.Acceptor", _initialContext);
        _acceptor = (XAcceptor) UnoRuntime.queryInterface(XAcceptor.class, acceptorObj);

        // Bridge factory
        Object bridgeFactoryObj = manager.createInstanceWithContext("com.sun.star.bridge.BridgeFactory", _initialContext);
        _bridgeFactory = (XBridgeFactory) UnoRuntime.queryInterface(XBridgeFactory.class, bridgeFactoryObj);
    }
    /**
     * called to shutdown the daemon
     * (all the terminating code is not included into this method,
     * there might be other code elsewhere such as in run() )
     */
    public synchronized void shutdown()
    {
        _isShutdowned 	= true;
        if (_acceptor != null)
            _acceptor.stopAccepting();
        _processPool.terminate();
        //System			.gc();
        //System			.runFinalization();
        logBigMessage	("OPEN OFFICE DAEMON STOPPED");
    }
    public static String extractContactInfo(String namevalue)
    {
        String[] 	list 	= namevalue.split(",");
        String 		host 	= "";
        String		port 	= "";
        for (int i = 0; i < list.length; i++)
        {
            String str = list[i];
            if (str.startsWith("peerHost"))
                host = str.split("=")[1];
            else if (str.startsWith("peerPort"))
                port = str.split("=")[1];
        }
        return host + ":" + port;
    }
    private void createBridgeWithEmptyPoolInstanceProvider(XConnection connection, String connectionDesc)
    {
        Logger.error(_processPool.getStateString() + " " + connectionDesc
                + " rejected, all workers are busy");
        try
        {
            _bridgeFactory.createBridge(
                "", "urp", connection, new EmptyPoolInstanceProvider());
        }
        catch (com.sun.star.bridge.BridgeExistsException ex)
        {
            Logger.error("Bridge already exists, cannot create new one");
            Logger.debug(ex);
        }
        catch (com.sun.star.lang.IllegalArgumentException ex)
        {
            Logger.error("Illegal argument for bridge creation");
            Logger.debug(ex);
        }
    }
    private void createBridgeWithOfficeInstanceProvider(XConnection connection,
            OfficeProcess process, String connectionDesc)
    {
        process.startUsage();
        Logger.info(_processPool.getStateString() + " -> "
                + process + " serves " + connectionDesc);

        /* XConnectionBroadcaster :
         * allows to add listeners to a connection. */
        XConnectionBroadcaster xConnectionBroadcaster = (XConnectionBroadcaster)
            UnoRuntime.queryInterface(XConnectionBroadcaster.class, connection);
        xConnectionBroadcaster.addStreamListener(new ConnectionListener(_processPool, process, connectionDesc));

        try
        {
            _bridgeFactory.createBridge(
                "", "urp", connection , new OfficeInstanceProvider(process));
        }
        catch (com.sun.star.bridge.BridgeExistsException ex)
        {
            Logger.error("Bridge already exists, cannot create new one");
            Logger.debug(ex);
        }
        catch (com.sun.star.lang.IllegalArgumentException ex)
        {
            Logger.error("Illegal argument for bridge creation");
            Logger.debug(ex);
        }
    }
    /**
     * Main loop that accepts connections from clients.
     * When a client is accepted, it calls an Instance provider.
     */
    private void mainLoop()
    {
        while (true)
        {
            XConnection connection = null;
            try
            {
                connection = _acceptor.accept(_config.acceptor);
            }
            catch (ConnectionSetupException ex)
            {
                if (isShutdowned()) // no need to throw an exception
                    break;
                else
                {
                    Logger.error("Connection setup exception when trying to accept new connection");
                    Logger.debug(ex);
                }
            }
            catch (AlreadyAcceptingException ex)
            {
                Logger.error("Already accepting connection, cannot connect");
                Logger.debug(ex);
            }
            catch (com.sun.star.lang.IllegalArgumentException ex)
            {
                Logger.error("Illegal argument for connection accepting");
                Logger.debug(ex);
            }
            if (connection == null) break;

            String connectionDesc = extractContactInfo(connection.getDescription());
            Logger.info("Incoming request for a worker from " + connectionDesc);
            Logger.debug("process pool size : " + _processPool.size());
            OfficeProcess process = _processPool.pop();
            if (process == null)
                createBridgeWithEmptyPoolInstanceProvider(connection, connectionDesc);
            else
                createBridgeWithOfficeInstanceProvider(connection, process, connectionDesc);
        }
    }
    /**
     * Creates all server instances at once.
     * There is one instance per user installation (OpenOffice server
     * doesn't allow multithreading, it's unstable).
     * The total number of users allowed in parallel is the number of
     * user installations.
     */
    public void fillProcessPool()
    {
        int index = 0;
        Iterator it = _config.userInstallation.iterator();
        while (it.hasNext())
        {
            String userId = (String) it.next();
            OfficeProcess p = new OfficeProcess(this, userId, new Integer(index));
            p.start();
            //Logger.info("Worker-" + index + ":" + p.toStringDetailed() + " started");
            _processPool.append(p);
            index++;
        }
        _processPool.waitTillReady();
        _processPool.initializationFinished();
        if (_config.adminNeeded())
        {
            _adminThread = new AdminAcceptorThread(this, _shutdownThread, _config.adminAcceptor);
            _adminThread.start();
        }

        Logger.info(_processPool.getStateString() + " All worker instances started");
    }
    /**
     * The entry method of the daemon, it calls init and mainLoop methods.
     */
    public void run()
    {
        fillProcessPool();
        Logger.info("Accepting on " + _config.acceptor);
        logBigMessage("OPEN OFFICE DAEMON STARTED");
        mainLoop();
        terminate();
    }
    /**
     * Called when starting the exiting process.
     * The shutdownThread will call shutdown() method.
     */
    private void terminate()
    {
        Logger.info("Accepting on " + _config.acceptor
                + " stopped, waiting for shutdownthread");

        if (_adminThread != null)
            _adminThread.cancel();

        try
        {
            if (_shutdownThread != null)
                _shutdownThread.cancel();
        }
        catch (Exception ex)
        {
            Logger.debug(ex);
        }

        try
        {
            if (_adminThread != null)
                _adminThread.join();
        }
        catch (Exception ex)
        {
            Logger.debug(ex);
        }

        Logger.info("Terminating normally");
    }
    /**
     * Logs a big message (more visible than normal logs)
     * It is written as a single string to be written in a single pass
     * (otherwise there would have been conflicts with other threads writing
     * at the same time)
     * @param msg the message that will be displayed, with decoration around
     */
    private void logBigMessage(String msg)
    {
        final String line = "======================================\n\n";
        String str =
             "\n" + line
            + "    " + msg + "\n\n"
            + line;
        Logger.info(str);
    }
    /**
     * Treatment to be done when the program is interrupted
     * ( warning : kill -9 does not work with it )
     */
    class ShutdownHook extends Thread
    {
        /**
         * Calls the shutdown method of the daemon
         * @see java.lang.Thread#run()
         */
        public void run ()
        {
            shutdown();
        }
    }
    /**
     * Command line version of the daemon
     * @param args described in CommandLine
     * @see com.anwrt.ooserver.daemon.CommandLine
     */
    public static void main(String[] args)
    {
        CommandLine.start(args);
    }
}

