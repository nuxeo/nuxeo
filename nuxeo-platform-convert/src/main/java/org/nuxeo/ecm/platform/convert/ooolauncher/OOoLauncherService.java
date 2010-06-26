package org.nuxeo.ecm.platform.convert.ooolauncher;

public interface OOoLauncherService {

    /**
     * Tells if OOoLaucher is available for starting OOo server if neeed
     * @return
     */
    //boolean isAvailable();

    /**
     * Tells if an OOo server is listening on target port
     *
     * @return
     */
    boolean isOOoListening();

    /**
     * Tells if OOo server has been started by the launcher
     *
     * @return
     */
    boolean isOOoLaunched();

    /**
     * Tells if {@link OOoLauncherService} is configured correctly
     * (if a valide OOo installation has been found)
     *
     * @return
     */
    boolean isConfigured();

    /**
     * Starts the OOo server process
     *
     * @return
     */
    boolean startOOo();

    /**
     * Starts the OOo server process and wait till ready to accept connection
     *
     * @return
     */
    boolean startOOoAndWaitTillReady();

    /**
     * Starts the OOo server process and wait till ready to accept connection
     *
     * @return
     */
    boolean startOOoAndWaitTillReady(int timeOutS);

    /**
     * Stops the OOo server process
     *
     * @return
     */
    boolean stopOOo();

    boolean stopOooAndWait(int timeOutS);

    /**
     * Blocks until OOo server is ready to accept connections
     *
     * @param timeOutS
     * @return
     */
    boolean waitTillReady(int timeOutS);

    /**
     * Blocks until OOo server is ready to accept connections
     *
     * @return
     */
    boolean waitTillReady();

}
