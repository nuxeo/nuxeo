package com.anwrt.ooserver.daemon;

import java.util.Timer;

import com.sun.star.bridge.BridgeExistsException;
import com.sun.star.connection.AlreadyAcceptingException;
import com.sun.star.connection.ConnectionSetupException;
import com.sun.star.connection.XAcceptor;
import com.sun.star.connection.XConnection;
import com.sun.star.uno.UnoRuntime;

/**
 * Accepts connections from admin daemon instances.
 * <br>
 *
 * @author <a href="mailto:oodaemon@extraserv.net">Jounayd Id Salah</a>
 */
public class AdminAcceptorThread extends Thread
{
    private Daemon				_daemon;
    private String 				_acceptString;
    private XAcceptor			_acceptor;
    private Timer				_shutdownThread = null;

    /** displayed tag used for logging */
    public static final String ADMIN_LOGGER_TAG = "#ADMIN# ";

    private static final int MAXIMUM_NUMBER_OF_ATTEMPTS = 50;

    /**
     * Creates an acceptor to further accept connections from admin daemon instances
     * @param daemon the current daemon
     * @param shutdownThread
     * @param acceptString the admin acceptor string (usually passed by the configuration file)
     */
    public AdminAcceptorThread(Daemon daemon, Timer shutdownThread,
             String acceptString)
    {
        super();
        _daemon			= daemon;
        _acceptString	= acceptString;
        _shutdownThread	= shutdownThread;

        try
        {
            Object acceptorObj	= daemon.getInitialContext().getServiceManager()
                .createInstanceWithContext("com.sun.star.connection.Acceptor", daemon.getInitialContext());
            _acceptor = (XAcceptor) UnoRuntime.queryInterface(XAcceptor.class, acceptorObj);
        }
        catch (Exception ex)
        {
            Logger.error(ADMIN_LOGGER_TAG + "Error getting acceptor");
            Logger.debug(ex);
        }
    }
    /**
     * Accept connections
     * @see java.lang.Thread#run()
     */
    public void run()
    {
        Logger.info(ADMIN_LOGGER_TAG + "started");
        int numberOfAttempts = 0;
        while (numberOfAttempts < MAXIMUM_NUMBER_OF_ATTEMPTS)
        {
            try
            {
                //have a chance to avoid flood the log
                Thread.sleep(10);
                numberOfAttempts ++;
                XConnection connexion = _acceptor.accept(_acceptString);
                if (connexion == null)
                    break;
                Logger.debug(ADMIN_LOGGER_TAG + "Accepted admin connection from "
                        + Daemon.extractContactInfo(connexion.getDescription()));
                _daemon.getBridgeFactory().createBridge(
                        "",
                        "urp",
                        connexion,
                        new AdminInstanceProvider(_daemon, _shutdownThread));
            }
            catch (AlreadyAcceptingException ex) // TODO this must be taken into account for the admin acceptor to work the best manner
            {
                Logger.error(ADMIN_LOGGER_TAG + "Already accepting connection");
                Logger.debug(ex);
            }
            catch (ConnectionSetupException ex)
            {
                if (_daemon.isShutdowned())
                    break;
                else
                {
                    Logger.error(ADMIN_LOGGER_TAG + ex);
                    Logger.debug(ex);
                }
            }
            catch (BridgeExistsException ex)
            {
                Logger.error(ADMIN_LOGGER_TAG + "Bridge already exists");
                Logger.debug(ex);
            }
            catch (com.sun.star.lang.IllegalArgumentException ex)
            {
                Logger.error(ADMIN_LOGGER_TAG + ex);
                Logger.debug(ex);
            }
            catch (InterruptedException ex)
            {
                Logger.error(ADMIN_LOGGER_TAG + ex);
                Logger.debug(ex);
            }
        }

        if (numberOfAttempts == MAXIMUM_NUMBER_OF_ATTEMPTS)
        {
            Logger.warning(ADMIN_LOGGER_TAG + "Could not connect after the maximum attempts ... exiting");
        }

        Logger.info(ADMIN_LOGGER_TAG + "terminating");
    }
    /**
     * Called to stop the admin thread
     */
    public void cancel()
    {
        _acceptor.stopAccepting();
    }
}
