package com.anwrt.ooserver.daemon;

import com.sun.star.bridge.XBridge;
import com.sun.star.connection.NoConnectException;
import com.sun.star.connection.XConnection;
import com.sun.star.connection.XConnector;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.lang.XComponent;

/**
 *
 * All the informations and management of an open office server instance
 * <br>
 *
 * @author <a href="mailto:oodaemon@extraserv.net">Jounayd Id Salah</a>
 */
public class OfficeProcess extends Thread
{
    private Config					_config;
    private String	 				_userId;
    private Integer 				_index;
    private Integer					_usage			= new Integer(0);
    private Long					_timestamp		= null;
    private XBridge					_bridge			= null;
    private XComponentContext		_context		= null;
    private Daemon 					_daemon;
    private Runtime					_runtime		= Runtime.getRuntime();
    private Process					_process		= null;
    private XConnection 			_connection		= null;

    /**
     * The time saved when the usage of the process started
     * @return date in milliseconds
     */
    public Long 				getTimestamp()		{ return _timestamp; }
    /**
     * Number of clients that used the process.
     * @return -
     */
    public Integer 				getUsage()			{ return _usage; }
    public String 				getUserId()			{ return _userId; }
    public Integer 				getIndex()			{ return _index; }
    public XComponentContext 	getContext()		{ return _context; }
    public XBridge				getBridge()			{ return _bridge; }

    public OfficeProcess(Daemon daemon, String userId, Integer index)
    {
        _daemon		= daemon;
        _config		= daemon.getConfig();
        _userId 	= userId;
        _index 		= index;
    }

    /**
     * It is necessary to create a new connector for each connection.
     * @return the new connector
     * @throws Exception
     */
    private XConnector getNewConnector() throws Exception
    {
        XConnector connector;

        XComponentContext context 	= _daemon.getInitialContext();

        Object connectorObj 		= context.getServiceManager()
            .createInstanceWithContext("com.sun.star.connection.Connector", context);
        connector = (XConnector) UnoRuntime.queryInterface(XConnector.class, connectorObj);

        return connector;
    }
    /**
     * Starts a new openOffice server instance represented by a java 'process'
     */
    public void start()
    {
        /*
         * tcpNoDelay : Corresponds to the socket option tcpNoDelay. For a UNO connection,
         * this parameter should be set to 1 (this is NOT the default ?
         * it must be added explicitly). If the default is used (0),
         * it may come to 200 ms delays at certain call combinations.
         */
        String[] command = new String[]
                {OSAbstractLayer.concatPaths(_config.officeProgramDirectoryPath, "soffice"),
                "-env:UserInstallation=" + _userId,
                "-headless",
                "-norestore",
                "-invisible",
                "-nofirststartwizard", // Added for OOo > 2.3
                //"-accept=" + getConnectString(index.intValue()) + ",tcpNoDelay=1;urp;", };
                "-accept=" + getConnectString() + ";urp;", };
        try
        {
            _process = _runtime.exec(command);
            if (_process != null)
                Logger.info("Worker instance " + _index + " started : " + _userId);
        }
        catch (java.io.IOException ex)
        {
            Logger.fatalError("cannot launch OpenOffice Server instance (command="
                    + command + ") ");
            Logger.debug(ex);
        }
    }
    /**
     * Kills the java process representing the openOffice server instance
     * TODO process.destroy() does not kill both soffice.bin and soffice.exe, one of
     * the two is still alive after the call
     */
    private void kill()
    {
        if (_process != null)
        {
            //os.kill( self.pid, signal.SIGKILL )
            _process.destroy();
            Logger.info(this + " killed");
            _process = null;
        }
    }
    /**
     * Closes the current server instance
     * (killed if not responding)
     */
    public void terminate()
    {
        if (_context != null)
        {
            Thread t = new TerminateThread(_context);
            Logger.info("terminating " + this);
            t.start();
            try
            {
                t.join(_config.sleepingDelay);
            }
            catch (InterruptedException ex)
            {
                Logger.debug(ex);
            }
            if (t.isAlive())
            {
                Logger.error(toStringDetailed() + " did not react on terminate, killing instance");
                kill();
            }
            else
            {
                Logger.info(this + " terminated");
            }

            _context = null;
        }
    }
    /**
     * Closes the server instance and tries to restart it
     * @return true if restarted and connected successfully
     */
    public boolean terminateAndRestart()
    {
        this.terminate();
        try
        {
            Thread.sleep(_config.shutdownDelay);
        }
        catch (InterruptedException ex)
        {
            Logger.debug(ex);
        }
        _runtime.gc();
        this.start();

        if (!waitTillReady(new Integer(_config.toleratedStartupTimePerInstance)))
        {
            Logger.error("could not restart instance " + this + ", terminating");
            return false;
        }
        _usage = new Integer(0);
        return true;
    }
    /**
     * Try to connect to one instance of open office using the current index of the OfficeProcess
     * object
     * @return true if connected
     */
    public boolean tryConnect()
    {
        try
        {
            // creating new connector because of a bug in jurt.jar/../Connector.java
            // see the issue at : http://www.openoffice.org/issues/show_bug.cgi?id=80947
            _connection = getNewConnector().connect(getConnectString());
            if (_connection == null)
                throw new NoConnectException("cannot connect (connector.connect(...) returned null)");

            _bridge 				= _daemon.getBridgeFactory().createBridge("", "urp", _connection, null);
            if (_bridge == null)
                throw new NoConnectException("cannot create bridge from bridge factory");

            Object contextObj 	= _bridge.getInstance("StarOffice.ComponentContext");
            _context 			= (XComponentContext) UnoRuntime.queryInterface(XComponentContext.class, contextObj);
            if (_context == null)
                throw new NoConnectException("cannot get instance of ComponentContext");

            return _context != null;
        }
        catch (com.sun.star.connection.NoConnectException ex)
        {
            Logger.debug(this + " not yet responsive : " + ex);
            Logger.info("Instance not responsive, retrying to connect soon");
        }
        catch (Exception ex)
        {
            Logger.error("couldn't connect to OpenOffice instance");
            Logger.debug(ex);
        }
        return false;
    }
    /**
     * Requests connection with server instance until it gets it or the timeout is reached
     * @param timeout try to connect in the given timeout, the method returns if the timeout
     * is reached
     * @return true if connected successfully
     */
    public boolean waitTillReady(Integer timeout)
    {
        long 		start 		= System.currentTimeMillis();
        boolean 	isTimeout 	= false;

        while (!tryConnect())
        {
            if (System.currentTimeMillis() - start > timeout.longValue())
            {
                isTimeout = true;
                break;
            }
            try
            {
                Thread.sleep(_config.sleepingDelay);
            }
            catch (InterruptedException ex) { /*DO NOTHING*/ }
        }

        if (isTimeout)
        {
            Logger.error("time-out for process attempting to connect");
            return false;
        }
        return true;
    }
    /**
     * Informs that a client has started to use the server instance.
     */
    public void startUsage()
    {
        _usage 		= new Integer(_usage.intValue() + 1);
        _timestamp 	= new Long(System.currentTimeMillis());
    }

    /**
     * Gets the duration since the client started to use the server instance
     * @return the duration
     */
    public long getUsageDuration()
    {
        return System.currentTimeMillis() - _timestamp.longValue();
    }
    /**
     * Informs the OfficeProcess that the client has finished using the server instance.
     */
    public void endUsage()
    {
        _timestamp = null;
    }
    /**
     * Creates a new thread that will test if the process is responsive.
     * @return true if the process responds correctly
     */
    public boolean isResponsive()
    {
        ResponsivenessChecker rc = new ResponsivenessChecker(_config, this);
        rc.start();
        return rc.isResponsive();
    }
    /**
     * Tests if it's necessary to restart an OpenOffice instance (not responsive or
     * maxUsageCount reached). If it is, it will try to restart it.
     * @return true if the process is restarted and connected
     */
    public boolean restartWhenNecessary()
    {
        if (!isResponsive())
        {
            Logger.info("process " + this + " not responsive anymore, restarting");
            return terminateAndRestart();
        }
        if (_usage.intValue() >= _config.calculateMaxUsageCount())
        {
            Logger.info("max usage count for instance " + this
                    + " reached, restarting");
            return terminateAndRestart();
        }
        return true;
    }

    /**
     * Builds a connection string that is used by oo servers.
     * Pipe is used by default : daemon should resides on the same
     * machine than open office server user installations
     * @return connection string
     */
    public String getConnectString()
    {
        return "pipe,name=daemon-instance-" + _index;
    }
    /**
     * small string
     * @see java.lang.Thread#toString()
     * @return small string
     */
    public String toString()
    {
        return "Worker-" + _index + "(" + _usage + " uses)";
    }
    /**
     * large information string
     * @return large information string
     */
    public String toStringDetailed()
    {
        return "<oood.OfficeProcess " + _userId + ";process=" + _process + ";connectStr="
         + getConnectString() + ",usage=" + _usage + ">";
    }
}
