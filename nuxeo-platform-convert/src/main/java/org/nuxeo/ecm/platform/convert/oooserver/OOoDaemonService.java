package org.nuxeo.ecm.platform.convert.oooserver;

public interface OOoDaemonService {


    /**
     * Returns true in built-in Daemon is Enabled by configuration
     *
     * @return
     */
    boolean isEnabled();

    /**
     * Returns true if OpenOffice is configured and found
     *
     * @return
     */
    boolean isConfigured();

    /**
     * Returns true if Daemon is running
     * @return
     */
    boolean isRunning();

    /**
     * Returns number of OpenOffice workers
     * @return
     */
    int getNbWorkers();


    /**
     * Starts the daemon and resturn immediatly
     * @return
     */
    int startDaemon();


    /**
     * Starts the Daemon and wait until Daemon is ready to accept calls
     * @return
     */
    boolean startDaemonAndWaitUntilReady();

    /**
     * Stops the Daemon and returns immediatly
     * @return
     */
    void stopDaemon();


    /**
     * Stops the Daemon and wait until it exists
     * @return
     */
    boolean stopDaemonAndWaitForCompletion();




}
