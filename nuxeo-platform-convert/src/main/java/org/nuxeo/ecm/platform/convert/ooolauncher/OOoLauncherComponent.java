package org.nuxeo.ecm.platform.convert.ooolauncher;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

import com.artofsolving.jodconverter.openoffice.connection.SocketOpenOfficeConnection;
import com.sun.star.frame.XDesktop;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

public class OOoLauncherComponent extends DefaultComponent implements
        OOoLauncherService, OOoConnectionManager {

    protected OOoLauncherDescriptor descriptor = new OOoLauncherDescriptor();

    protected OOoConfigHelper configHelper = null;

    protected Log log = LogFactory.getLog(OOoLauncherComponent.class);

    protected Process OOoProcess = null;

    protected boolean started = false;

    protected OOoConfigHelper getConfigHelper() {
        if (configHelper == null) {
            configHelper = new OOoConfigHelper(descriptor);
        }
        return configHelper;
    }

    public boolean isOOoLaunched() {
        return started;
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        stopOOo();
    }

    // public boolean isOOoRunning() {
    // if (!isOOoLaunched() || OOoProcess==null) {
    // log.debug("Can not check if OOo process is running as it is not started");
    // return false;
    // }
    //
    // return true;
    //
    //
    //
    // int exitCode=0;
    // try {
    // exitCode = OOoProcess.exitValue();
    // }
    // catch (IllegalThreadStateException e) {
    // return true;
    // }
    //
    // log.info("OOo process exited with return code" + exitCode);
    // OOoProcess=null;
    // started=false;
    //
    // return false;
    // }

    public boolean waitTillReady() {
        return waitTillReady(descriptor.getOooStartupTimeOut());
    }

    public Integer getProcessExitCode() {
        int exitCode=0;
        try {
            exitCode = OOoProcess.exitValue();
        }
        catch (IllegalThreadStateException e) {
            return null;
        }
        return exitCode;
    }

    public boolean waitTillReady(int timeOutS) {
        for (int i = 0; i < timeOutS; i++) {
            if (isOOoListening()) {
                return true;
            }
            try {
                Thread.sleep(1000);
                log.debug("Process exit code = " + getProcessExitCode());
                if (i % 15 == 0) {
                    log.info("re-try to connect to OOo server");
                }
            } catch (InterruptedException e) {
                log.error("Unxpected exception", e);
            }
        }
        return false;
    }

    protected boolean isPortFree() {

        Socket socket = null;
        try {
            socket = new Socket();
            InetSocketAddress addr = new InetSocketAddress(descriptor
                    .getOooListenerIP(), descriptor.getOooListenerPort());
            socket.connect(addr, 100);
            socket.close();
            log.debug("tcp connect succeeded => socket is not free");
            return false;
        } catch (Throwable t) {
            log.debug("Error when trying to connect to OOo TCP port" + t.getMessage());
            log.debug("Stocket seems to be free");
            return true;
        } finally {
            if (socket!=null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    log.trace("Error when closing socket", e);
                }
            }
        }
    }

    public boolean isOOoListening() {

        if (isPortFree()) {
            return false;
        }
        SocketOpenOfficeConnection oooConn = new SocketOpenOfficeConnection(
                descriptor.getOooListenerIP(), descriptor.getOooListenerPort());

        try {
            log.debug("try to connect to OOo server via SocketOpenOfficeConnection");
            oooConn.connect();
            log.debug("SocketOpenOfficeConnection succeed");
        } catch (ConnectException e1) {
            return false;
        } finally {
            if (oooConn != null && oooConn.isConnected()) {
                oooConn.disconnect();
                oooConn = null;
            }
        }

        return true;
    }

    public boolean startOOoAndWaitTillReady() {
        return startOOoAndWaitTillReady(descriptor.getOooStartupTimeOut());
    }

    public boolean startOOoAndWaitTillReady(int timeOutS) {

        boolean started = startOOo();
        if (!started) {
            return false;
        }
        return waitTillReady(timeOutS);
    }

    public boolean startOOo() {

        if (started) {
            return true;
        }

        String[] command = getConfigHelper().getOOoLaunchCommand();

        try {
            OOoProcess = Runtime.getRuntime().exec(command);
            started = true;
        } catch (IOException e) {
            log.error("Unable to start OOo process", e);
            return false;
        }

        return started;
    }

    public boolean stopOooAndWait(int timeOutS) {
        if (!isOOoLaunched() || OOoProcess == null) {
            log.debug("Can not stop OOo as it is not running (or not started by the launcher)");
            return false;
        }

        boolean stoped = stopOOo();
        if (!stoped) {
            return false;
        }

        for (int i = 0; i < timeOutS; i++) {
            if (isPortFree()) {
                return true;
            }
            try {
                Thread.sleep(1000);
                if (i % 15 == 0) {
                    log.info("Waiting for server to stop accessing connections");
                }
            } catch (InterruptedException e) {
                // NOP
            }
        }
        return false;
    }

    public boolean stopOOo() {
        if (!isOOoLaunched() || OOoProcess == null) {
            log
                    .debug("Can not stop OOo as it is not running (or not started by the launcher)");
            return false;
        }

        if (sharedConnection!=null && sharedConnection.isConnected()) {
            sharedConnection.disconnect();
        }

        SocketOpenOfficeConnection oooConn = new SocketOpenOfficeConnection(
                descriptor.getOooListenerIP(), descriptor.getOooListenerPort());

        try {
            oooConn.connect();
            Object od = oooConn.getDesktop();
            XComponentContext ctx = oooConn.getComponentContext();
            Object desktopObj = ctx.getServiceManager()
                    .createInstanceWithContext("com.sun.star.frame.Desktop",
                            ctx);
            XDesktop desktop = (XDesktop) UnoRuntime.queryInterface(
                    XDesktop.class, desktopObj);
            desktop.terminate();
            oooConn.disconnect();
        } catch (Exception e) {
            log.error("Error while killing OOo", e);
            return false;
        } finally {
            if (oooConn != null && oooConn.isConnected()) {
                oooConn.disconnect();
            }
            oooConn = null;
        }

        OOoProcess.destroy();
        started = false;
        OOoProcess = null;
        return true;
    }

    public boolean isAvailable() {
        return (isConfigured() && isPortFree());
    }

    public boolean isEnabled() {
        return descriptor.isEnabled() && isConfigured();
    }

    public boolean isConfigured() {
        return getConfigHelper().isConfiguredOk();
    }

    // ConnectionManager interface

    protected boolean failedToConnect=false;

    protected Boolean canGetConnection = null;

    protected SocketOpenOfficeConnection sharedConnection;

    private static final Lock conLock = new ReentrantLock();

    public boolean canGetConnection() {

        if (canGetConnection!=null) {
            return canGetConnection;
        }

        if (failedToConnect) {
            // avoid multiple attempts
            return false;
        }

        if (isConfigured()) {
            canGetConnection=true;
        } else {
            if (!isPortFree()) {
                log.info("Launcher is not enabled, but port is listening, assuming manual startup");
                canGetConnection =  true;
            } else {
                log.warn("Launcher is not enabled, no OOo instance seems to be listening on the target port");
                canGetConnection =  false;
            }
        }
        return canGetConnection;
    }

    private boolean acquireLock() {
        boolean acquired = false;
        try {
            acquired = conLock.tryLock(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Cannot acquire an OOo connection");
        } finally {
            if (!acquired) {
                log.error("Cannot acquire an OOo connection :: timeout");
            } else {
                log.trace("Acquired connection lock");
            }
        }
        return acquired;
    }

    private void releaseLock() {
        conLock.unlock();
        log.trace("Release connection lock");
    }

    public SocketOpenOfficeConnection getConnection() {

        if (!canGetConnection()) {
            return null;
        }

        boolean locked = acquireLock();
        if (!locked) {
            return null;
        }

        if (sharedConnection!=null) {
            return sharedConnection;
        }

        if (isEnabled()) {
            // use the launcher
            if (!isOOoLaunched()) {
                startOOoAndWaitTillReady();
            }
        }

        sharedConnection = new SocketOpenOfficeConnection(
                descriptor.getOooListenerIP(), descriptor.getOooListenerPort());

        try {
            log.debug("try to connect to OOo server via SocketOpenOfficeConnection");
            sharedConnection.connect();
            log.debug("SocketOpenOfficeConnection succeed");
        } catch (ConnectException e) {
            log.error("Error during Ooo connection", e);
            sharedConnection=null;
        } finally {
        }

        return sharedConnection;
    }

    public void releaseConnection(SocketOpenOfficeConnection connection) {

        releaseLock();
    }

}
