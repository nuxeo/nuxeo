package org.nuxeo.ecm.platform.convert.oooserver;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("OOoServer")
public class OOoServerDescriptor implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;


    @XNode("enableDaemon")
    private boolean enabledDaemon=true;

    @XNode("oooListenIP")
    private String oooListenerIP="127.0.0.1";

    @XNode("oooListenPort")
    private int oooListenerPort=8100;

    @XNode("oooDaemonListenPort")
    private int oooDaemonListenerPort=8101;

    @XNode("oooWorkers")
    private int oooWorkers = 1;

    @XNode("oooInstallationPath")
    private String oooInstallationPath=null;

    @XNode("oooWorkersRecycleInterval")
    private int oooRecycleInterval=10;

    @XNode("autoStart")
    private boolean autoStart=false;

    @XNode("jpipeLibPath")
    private String jpipeLibPath=null;

    @XNode("logInfoAsDebug")
    private boolean logInfoAsDebug = true;

    public boolean getLogInfoAsDebug() {
        return logInfoAsDebug;
    }

    public void setLogInfoAsDebug(boolean logInfoAsDebug) {
        this.logInfoAsDebug = logInfoAsDebug;
    }

    public String getJpipeLibPath() {
        return jpipeLibPath;
    }

    public void setJpipeLibPath(String jpipeLibPath) {
        this.jpipeLibPath = jpipeLibPath;
    }

    public boolean isDaemonEnabled() {
        return enabledDaemon;
    }

    public void setDaemonEnabled(boolean enabledDaemon) {
        this.enabledDaemon = enabledDaemon;
    }

    public String getOooListenerIP() {
        return oooListenerIP;
    }

    public void setOooListenerIP(String oooListenerIP) {
        this.oooListenerIP = oooListenerIP;
    }

    public int getOooListenerPort() {
        return oooListenerPort;
    }

    public void setOooListenerPort(int oooListenerPort) {
        this.oooListenerPort = oooListenerPort;
    }

    public int getOooDaemonListenerPort() {
        return oooDaemonListenerPort;
    }

    public void setOooDaemonListenerPort(int oooDaemonListenerPort) {
        this.oooDaemonListenerPort = oooDaemonListenerPort;
    }

    public int getOooWorkers() {
        return oooWorkers;
    }

    public void setOooWorkers(int oooWorkers) {
        this.oooWorkers = oooWorkers;
    }

    public String getOooInstallationPath() {
        return oooInstallationPath;
    }

    public void setOooInstallationPath(String oooInstallationPath) {
        this.oooInstallationPath = oooInstallationPath;
    }

    public int getOooRecycleInterval() {
        return oooRecycleInterval;
    }

    public void setOooRecycleInterval(int oooRecycleInterval) {
        this.oooRecycleInterval = oooRecycleInterval;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }




}
