/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */
package org.nuxeo.wss;

/**
 * Holds Const values and configuration used for WSS
 *
 * @author Thierry Delprat
 */
public class WSSConfig {

    protected static WSSConfig instance;

    public static final String DEFAULT_TS_SERVER_VERSION = "12.0.0.6219";

    protected String TSServerVersion = DEFAULT_TS_SERVER_VERSION;

    public static final String DEFAULT_WSS_SERVER_VERSION = "12.0.0.6421";

    protected String WSSServerVersion = DEFAULT_WSS_SERVER_VERSION;

    public static final String DEFAULT_FP_SERVER_VERSION = "12.0.0.000";

    protected String FPServerVersion = DEFAULT_FP_SERVER_VERSION;

    protected String wssBackendFactoryClassName = "org.nuxeo.wss.spi.dummy.DummyBackendFactory";

    protected String contextPath = "";

    protected boolean hostFPExtensionAtRoot = true;

    public static final String DEFAULT_ENCODING = "org.nuxeo.wss.handler.windows.encoding";

    public synchronized static WSSConfig instance() {
        if (instance == null) {
            instance = new WSSConfig();
        }
        return instance;
    }

    public String getWSSServerVersion() {
        return WSSServerVersion;
    }

    public String getWSSServerVersionMajor() {
        return WSSServerVersion.split("\\.")[0];
    }

    public String getWSSServerVersionMinor() {
        return WSSServerVersion.split("\\.")[1];
    }

    public String getWSSServerVersionPhase() {
        return WSSServerVersion.split("\\.")[2];
    }

    public String getWSSServerVersionBuild() {
        return WSSServerVersion.split("\\.")[3];
    }

    public String getTSServerVersion() {
        return TSServerVersion;
    }

    public String getTSServerVersionMajor() {
        return TSServerVersion.split("\\.")[0];
    }

    public String getTSServerVersionMinor() {
        return TSServerVersion.split("\\.")[1];
    }

    public String getTSServerVersionPhase() {
        return TSServerVersion.split("\\.")[2];
    }

    public String getTSServerVersionBuild() {
        return TSServerVersion.split("\\.")[3];
    }

    public void setWSSServerVersion(String serverVersion) {
        WSSServerVersion = serverVersion;
    }

    public String getFPServerVersion() {
        return FPServerVersion;
    }

    public String getTZOffset() {
        return "+0200";
    }

    public String getLang() {
        return "1033";
    }

    public String getWssBackendFactoryClassName() {
        return wssBackendFactoryClassName;
    }

    public void setWssBackendFactoryClassName(String wssBackendFactoryClassName) {
        this.wssBackendFactoryClassName = wssBackendFactoryClassName;
    }

    public String getResourcesUrlPattern() {
        return "/resources/";
    }

    public String getResourcesBasePath() {
        return "webresources/";
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String path) {
        contextPath = path;
        if (!contextPath.endsWith("/")) {
            contextPath = contextPath + "/";
        }
        if (contextPath.startsWith("/")) {
            contextPath = contextPath.substring(1);
        }
    }

    public String getWSSUrlPrefix() {
        return "_vti_bin";
    }

    public boolean isHostFPExtensionAtRoot() {
        return hostFPExtensionAtRoot;
    }

    public void setHostFPExtensionAtRoot(boolean hostFPExtensionAtRoot) {
        this.hostFPExtensionAtRoot = hostFPExtensionAtRoot;
    }

}
