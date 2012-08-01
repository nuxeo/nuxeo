/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.connect.client.jsf;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.connect.client.status.ConnectStatusHolder;
import org.nuxeo.connect.client.status.ConnectUpdateStatusInfo;
import org.nuxeo.connect.client.status.SubscriptionStatusWrapper;
import org.nuxeo.connect.connector.NuxeoClientInstanceType;
import org.nuxeo.connect.connector.http.ConnectUrlConfig;
import org.nuxeo.connect.data.ConnectProject;
import org.nuxeo.connect.data.SubscriptionStatusType;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier.InvalidCLID;
import org.nuxeo.connect.identity.TechnicalInstanceIdentifier;
import org.nuxeo.connect.registration.ConnectRegistrationService;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.runtime.api.Framework;

/**
 * Seam Bean to expose Connect Registration operations.
 * <ul>
 * <li>getting status
 * <li>registering
 * <li>...
 * </ul>
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@Name("connectStatus")
@Scope(ScopeType.CONVERSATION)
public class ConnectStatusActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String CLIENT_BANNER_TYPE = "clientSideBanner";

    public static final String SERVER_BANNER_TYPE = "serverSideBanner";

    private static final Log log = LogFactory.getLog(ConnectStatusActionBean.class);

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    protected String login;

    protected String password;

    protected String registredProject;

    protected String instanceType;

    protected String instanceDescription;

    protected boolean loginValidated = false;

    protected String CLID;

    protected ConnectUpdateStatusInfo connectionStatusCache;

    public String getRegistredCLID() throws Exception {
        if (isRegistred()) {
            return LogicalInstanceIdentifier.instance().getCLID();
        } else {
            return null;
        }
    }

    public String getCLID() {
        return CLID;
    }

    public void setCLID(String cLID) {
        CLID = cLID;
    }

    public String unregister() {
        LogicalInstanceIdentifier.cleanUp();
        resetRegister();
        return null;
    }

    public List<SelectItem> getInstanceTypes() {
        List<SelectItem> types = new ArrayList<SelectItem>();
        for (NuxeoClientInstanceType itype : NuxeoClientInstanceType.values()) {
            SelectItem item = new SelectItem(itype.getValue(),
                    "label.instancetype." + itype.getValue());
            types.add(item);
        }
        return types;
    }

    @Factory(scope = ScopeType.EVENT, value = "connectLoginValidated")
    public boolean isLoginValidated() {
        return loginValidated;
    }

    protected ConnectRegistrationService getService() {
        return Framework.getLocalService(ConnectRegistrationService.class);
    }

    public String getRegistredProject() {
        return this.registredProject;
    }

    public void setRegistredProject(String registredProject) {
        this.registredProject = registredProject;
    }

    public String getConnectLogin() {
        return this.login;
    }

    public void setConnectLogin(String login) {
        this.login = login;
    }

    public String getConnectPassword() {
        return this.password;
    }

    public void setConnectPassword(String password) {
        this.password = password;
    }

    @Factory(scope = ScopeType.APPLICATION, value = "registredConnectInstance")
    public boolean isRegistred() {
        return getService().isInstanceRegistred();
    }

    protected void flushContextCache() {
        // A4J and Event cache don't play well ...
        Contexts.getEventContext().remove("connectLoginValidated");
        Contexts.getEventContext().remove("projectsForRegistration");
        Contexts.getApplicationContext().remove("registredConnectInstance");
        Contexts.getApplicationContext().remove("connectUpdateStatusInfo");
    }

    public void validateLogin() {
        if (login == null || password == null) {
            facesMessages.addToControl("login", StatusMessage.Severity.WARN,
                    messages.get("label.empty.loginpassword"));
            loginValidated = false;
            flushContextCache();
            return;
        }
        List<ConnectProject> prjs = getProjectsAvailableForRegistration();
        if (prjs == null || prjs.size() == 0) {
            facesMessages.addToControl("online", StatusMessage.Severity.WARN,
                    messages.get("label.bad.loginpassword.or.noproject"));
            loginValidated = false;
            flushContextCache();
            return;
        }
        flushContextCache();
        loginValidated = true;
    }

    @Factory(value = "connectServerReachable", scope = ScopeType.EVENT)
    public boolean isConnectServerReachable() {
        return !ConnectStatusHolder.instance().getStatus().isConnectServerUnreachable();
    }

    public String refreshStatus() {
        ConnectStatusHolder.instance().getStatus(true);
        flushContextCache();
        return null;
    }

    public SubscriptionStatusWrapper getStatus() {
        return ConnectStatusHolder.instance().getStatus();
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public String getInstanceDescription() {
        return instanceDescription;
    }

    public void setInstanceDescription(String instanceDescription) {
        this.instanceDescription = instanceDescription;
    }

    public String enterConnectCredentials() {
        // XXX security checks
        if (login == null) {
            return "connectCredential";
        }
        return "projectListing";
    }

    @Factory(scope = ScopeType.EVENT, value = "projectsForRegistration")
    public List<ConnectProject> getProjectsAvailableForRegistration() {
        List<ConnectProject> projects = new ArrayList<ConnectProject>();
        if (login != null) {
            try {
                projects = getService().getAvailableProjectsForRegistration(
                        login, password);
                if (!projects.isEmpty()) {
                    Collections.sort(projects,
                            new Comparator<ConnectProject>() {
                                @Override
                                public int compare(ConnectProject o1,
                                        ConnectProject o2) {
                                    return o1.getName().compareTo(o2.getName());
                                }
                            });
                }
            } catch (Exception e) {
                log.error("Error while getting remote project", e);
            }
        }
        return projects;
    }

    public String resetRegister() {
        login = null;
        password = null;
        loginValidated = false;
        registredProject = null;
        instanceDescription = null;
        flushContextCache();
        return null;
    }

    public String register() {
        if (registredProject == null) {
            facesMessages.addToControl("online", StatusMessage.Severity.WARN,
                    messages.get("label.empty.project"));
            return null;
        }
        autofillInstanceDescription();
        try {
            getService().remoteRegisterInstance(login, password,
                    registredProject,
                    NuxeoClientInstanceType.fromString(instanceType),
                    instanceDescription);
        } catch (Exception e) {
            facesMessages.addToControl("online", StatusMessage.Severity.ERROR,
                    messages.get("label.connect.registrationError"));
            log.error("Error while registering instance", e);
        }

        flushContextCache();
        ConnectStatusHolder.instance().flush();
        return null;
    }

    /**
     * @since 5.6
     */
    protected void autofillInstanceDescription() {
        if (instanceDescription == null || instanceDescription.isEmpty()) {
            instanceDescription = login + "'s " + instanceType + " instance";
        }
    }

    public String getCTID() throws Exception {
        return TechnicalInstanceIdentifier.instance().getCTID();
    }

    public String localRegister() {
        autofillInstanceDescription();
        try {
            getService().localRegisterInstance(CLID, instanceDescription);
        } catch (InvalidCLID e) {
            facesMessages.addToControl("offline_clid",
                    StatusMessage.Severity.WARN,
                    messages.get("label.connect.wrongCLID"));
        } catch (IOException e) {
            facesMessages.addToControl("offline_clid",
                    StatusMessage.Severity.ERROR,
                    messages.get("label.connect.registrationError"));
            log.error("Error while registering instance locally", e);
        }
        flushContextCache();
        return null;
    }

    protected Blob packageToUpload;

    protected String packageFileName;

    public String getPackageFileName() {
        return packageFileName;
    }

    public void setPackageFileName(String packageFileName) {
        this.packageFileName = packageFileName;
    }

    public Blob getPackageToUpload() {
        return packageToUpload;
    }

    public void setPackageToUpload(Blob packageToUpload) {
        this.packageToUpload = packageToUpload;
    }

    public void uploadPackage() throws Exception {
        if (packageToUpload == null) {
            facesMessages.add(StatusMessage.Severity.WARN,
                    "label.connect.nofile");
            return;
        }
        PackageUpdateService pus = Framework.getLocalService(PackageUpdateService.class);
        File tmpFile = File.createTempFile("upload", "nxpkg");
        packageToUpload.transferTo(tmpFile);
        try {
            pus.addPackage(tmpFile);
        } catch (Exception e) {
            facesMessages.add(
                    StatusMessage.Severity.ERROR,
                    messages.get("label.connect.wrong.package" + ":"
                            + e.getMessage()));
            return;
        } finally {
            tmpFile.delete();
            packageFileName = null;
            packageToUpload = null;
        }
    }

    public ConnectUpdateStatusInfo getDynamicConnectUpdateStatusInfo() {
        HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String bannerType = req.getParameter("bannerType");
        if ("unregistered".equals(bannerType)) {
            return ConnectUpdateStatusInfo.unregistered();
        } else if ("notreachable".equals(bannerType)) {
            return ConnectUpdateStatusInfo.connectServerUnreachable();
        } else if ("notvalid".equals(bannerType)) {
            return ConnectUpdateStatusInfo.notValid();
        } else if ("ok".equals(bannerType)) {
            return ConnectUpdateStatusInfo.ok();
        }
        return getConnectUpdateStatusInfo();
    }

    @Factory(scope = ScopeType.APPLICATION, value = "connectUpdateStatusInfo")
    public ConnectUpdateStatusInfo getConnectUpdateStatusInfo() {
        if (connectionStatusCache == null) {
            try {
                if (!isRegistred()) {
                    connectionStatusCache = ConnectUpdateStatusInfo.unregistered();
                } else {
                    if (isConnectServerReachable()) {
                        if (getStatus().isError()) {
                            connectionStatusCache = ConnectUpdateStatusInfo.connectServerUnreachable();
                        } else {
                            if (ConnectStatusHolder.instance().getStatus().status() == SubscriptionStatusType.OK) {
                                connectionStatusCache = ConnectUpdateStatusInfo.ok();
                            } else {
                                connectionStatusCache = ConnectUpdateStatusInfo.notValid();
                            }
                        }
                    } else {
                        connectionStatusCache = ConnectUpdateStatusInfo.connectServerUnreachable();
                    }
                }
            } catch (Exception e) {
                log.error(e);
                connectionStatusCache = null;
            }
        }
        return connectionStatusCache;
    }

    @Factory("nuxeoConnectUrl")
    public String getConnectServerUrl() {
        return ConnectUrlConfig.getBaseUrl();
    }

}
