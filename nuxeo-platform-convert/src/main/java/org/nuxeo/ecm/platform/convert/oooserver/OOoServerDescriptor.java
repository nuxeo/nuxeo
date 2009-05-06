/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.convert.oooserver;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("OOoServer")
public class OOoServerDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("enableDaemon")
    private boolean enabledDaemon = true;

    @XNode("oooListenIP")
    private String oooListenerIP = "127.0.0.1";

    @XNode("oooListenPort")
    private int oooListenerPort = 8100;

    @XNode("oooDaemonListenPort")
    private int oooDaemonListenerPort = 8101;

    @XNode("oooWorkers")
    private int oooWorkers = 1;

    @XNode("oooInstallationPath")
    private String oooInstallationPath;

    @XNode("oooWorkersRecycleInterval")
    private int oooRecycleInterval = 10;

    @XNode("autoStart")
    private boolean autoStart = false;

    @XNode("jpipeLibPath")
    private String jpipeLibPath;

    @XNode("oooServerStartTimeout")
    private int oooServerStartTimeout = 60;

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

    public int getOooServerStartTimeout() {
        return oooServerStartTimeout;
    }

}
