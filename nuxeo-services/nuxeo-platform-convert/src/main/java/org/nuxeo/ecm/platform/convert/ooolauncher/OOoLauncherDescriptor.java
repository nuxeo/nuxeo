package org.nuxeo.ecm.platform.convert.ooolauncher;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("OOoLauncher")
public class OOoLauncherDescriptor {

    @XNode("oooListenIP")
    private String oooListenerIP = "127.0.0.1";

    @XNode("oooListenPort")
    private int oooListenerPort = 8100;

    @XNode("oooStartupTimeOut")
    private int oooStartupTimeOut = 60;

    @XNode("oooInstallationPath")
    private String oooInstallationPath;

    @XNode("startOOoAtServiceStartup")
    private boolean startOOoAtServiceStartup=false;

    @XNode("enabled")
    private boolean enabled=true;


    public boolean isEnabled() {
        return enabled;
    }

    public String getOooListenerIP() {
        return oooListenerIP;
    }

    public int getOooListenerPort() {
        return oooListenerPort;
    }

    public String getOooInstallationPath() {
        return oooInstallationPath;
    }

    public int getOooStartupTimeOut() {
        return oooStartupTimeOut;
    }

    public boolean getStartOOoAtServiceStartup() {
        return startOOoAtServiceStartup;
    }


}
