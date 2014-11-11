package org.nuxeo.ecm.platform.convert.ooolauncher;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.convert.oooserver.OOoDaemonManagerComponent;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

import com.artofsolving.jodconverter.openoffice.connection.SocketOpenOfficeConnection;
import com.sun.star.frame.XDesktop;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

public class OOoLauncherComponent extends DefaultComponent implements
        OOoLauncherService, OOoConnectionManager, FrameworkListener {

    protected static String CONFIG_EP = "oooLauncherConfig";

    protected OOoLauncherDescriptor descriptor = new OOoLauncherDescriptor();

    protected OOoConfigHelper configHelper = null;

    protected Log log = LogFactory.getLog(OOoLauncherComponent.class);

    protected Process OOoProcess = null;

    protected boolean started = false;

    protected int connUsageNb=0;
    protected static final int maxConnUsage=50;

    public OOoLauncherDescriptor getDescriptor() {
        return descriptor;
    }

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
    public void registerContribution(Object contribution, String extensionPoint,
            ComponentInstance contributor) throws Exception {
        if (CONFIG_EP.equals(extensionPoint)) {
            OOoLauncherDescriptor desc = (OOoLauncherDescriptor) contribution;
            descriptor = desc;
        }
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        context.getRuntimeContext().getBundle().getBundleContext().addFrameworkListener(this);
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        stopOOo();
        File oooDir = new File(OOoConfigHelper.getUserDir());
        if (oooDir.exists()) {
            FileUtils.deleteTree(oooDir);
        }
    }

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
            log.debug("tcp connect succeeded => socket is not free => Ooo is listening ");
            return false;
        } catch (Throwable t) {
            log.trace("Error when trying to connect to OOo TCP port " + t.getMessage());
            log.debug("Stocket seems to be free => ooo is not (yet) listening");
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

    protected class OOoConnectorThread implements Runnable {

        private boolean connectedOk=false;
        protected SocketOpenOfficeConnection conn;

        public OOoConnectorThread(){
             conn = new SocketOpenOfficeConnection(
                     descriptor.getOooListenerIP(), descriptor.getOooListenerPort());
        }

        public void run() {
            try {
                log.debug("Try to connect using SocketOpenOfficeConnection is a separated thread");
                conn.connect();
                log.debug("SocketOpenOfficeConnection succeeded");
                connectedOk = true;
            } catch (Exception e) {
                log.error("Error while connecting to OOo", e);
                conn = null;
            }
        }

        public SocketOpenOfficeConnection getConn() {
            if (connectedOk) {
                return conn;
            } else {
                return null;
            }
        }
    }

    protected SocketOpenOfficeConnection safeGetConnection() {
        OOoConnectorThread thread = new OOoConnectorThread();
        Thread connThread = new Thread(thread);
        connThread.start();
        try {
            connThread.join(3000);
        } catch (InterruptedException e) {
            return null;
        }

        SocketOpenOfficeConnection conn =thread.getConn();

        if (conn==null) {
            log.debug("Killing conn thread");
            connThread.interrupt();
            try {
                connThread.join(1000);
                log.debug("Conn Thread terminated");
            } catch (InterruptedException e) {
                log.error("Error while waiting for connThread to exit");
            }
        }
        return conn;
    }


    public boolean isOOoListening() {

        if (isPortFree()) {
            return false;
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // NOP
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
            log.warn("Unable to kill propertly Ooo process, ... testing if it it still running ...");
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
            log.debug("Can not stop OOo as it is not running (or not started by the launcher)");
            return false;
        }

        SocketOpenOfficeConnection oooConn = null;

        try {
            // in order to stop OOo, we need to connect to it !!!

            if (sharedConnection!=null && sharedConnection.isConnected()) {
                oooConn = sharedConnection;
            } else {
                oooConn = new SocketOpenOfficeConnection(
                        descriptor.getOooListenerIP(), descriptor.getOooListenerPort());
                try {
                    oooConn.connect();
                } catch (ConnectException e) {
                    log.error("Unable to connect to Ooo in order to kill it !!!");
                    return false;
                }
            }

            try {
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
            }
        }
        finally {
            if (oooConn != null && oooConn.isConnected()) {
                oooConn.disconnect();
            }
            oooConn = null;
            if (sharedConnection!=null && sharedConnection.isConnected()) {
                sharedConnection.disconnect();
            }
            sharedConnection = null;
            OOoProcess.destroy();
            started = false;
            OOoProcess = null;
        }
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

    //****************************
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
                canGetConnection = true;
            } else {
                log.warn("Launcher is not enabled, no OOo instance seems to be listening on the target port");
                canGetConnection = false;
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

        if (sharedConnection != null) {
            return sharedConnection;
        }

        if (isEnabled()) {
            // use the launcher
            if (!isOOoLaunched()) {
                if (!isPortFree()) {
                    log.info("OOo port is not free : OOo has been started from outside ?");
                } else {
                    log.info("Try to start OOo process");
                    boolean ready = startOOoAndWaitTillReady();
                    if (!ready) {
                        log.error("Unable to start Ooo process");
                        failedToConnect=true;
                        return null;
                    }
                }
            }
        }

        sharedConnection = safeGetConnection();
        if (sharedConnection == null) {
            log.error("Unable to connect to OOo server");
            failedToConnect = true;
            releaseLock();
        }

        return sharedConnection;
    }

    public void releaseConnection(SocketOpenOfficeConnection connection) {
        releaseLock();
        connUsageNb += 1;
        if (connUsageNb > maxConnUsage) {
            sharedConnection.disconnect();
            sharedConnection = null;
            connUsageNb = 0;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // NOP
            }
        }
    }

    public void frameworkEvent(FrameworkEvent event) {
        if (event.getType() == FrameworkEvent.STARTED) {

            ClassLoader jbossCL = Thread.currentThread().getContextClassLoader();
            ClassLoader nuxeoCL = OOoDaemonManagerComponent.class.getClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(nuxeoCL);
                log.debug("OOoLauncher Service initialization");
                if (descriptor.getStartOOoAtServiceStartup()) {
                    if (isConfigured()) {
                        log.info("Starting OOo server process");
                        startOOo();
                    } else {
                        log.info("OOo Server is not well configured, can not start OpenOffice server processs");
                    }
                }
            }
            finally {
                Thread.currentThread().setContextClassLoader(jbossCL);
                log.debug("JBoss ClassLoader restored");
            }
        }
    }

}
