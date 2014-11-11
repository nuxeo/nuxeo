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

package org.nuxeo.connect.client.jsf;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.connect.client.status.ConnectStatusHolder;
import org.nuxeo.connect.client.status.SubscriptionStatusWrapper;
import org.nuxeo.connect.connector.NuxeoClientInstanceType;
import org.nuxeo.connect.data.ConnectProject;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier;
import org.nuxeo.connect.identity.TechnicalInstanceIdentifier;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier.InvalidCLID;
import org.nuxeo.connect.registration.ConnectRegistrationService;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.runtime.api.Framework;

/**
 * Seam Bean to expose Connect Registration operations.
 * <ul>
 * <li>getting status
 * <li>registering
 * <li> ...
 * </ul>
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@Name("connectStatus")
@Scope(CONVERSATION)
public class ConnectStatusActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(ConnectStatusActionBean.class);

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    protected String login;

    protected String password;

    protected String registredProject;

    protected String instanceType;

    protected String instanceDescription;

    protected boolean loginValidated = false;

    protected String CLID;

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

    @Factory(scope = ScopeType.EVENT, value = "registredConnectInstance")
    public boolean isRegistred() {
        return getService().isInstanceRegistred();
    }

    protected void flushEventCache() {
        // A4J and Event cache don't play well ...
        Contexts.getEventContext().remove("connectLoginValidated");
        Contexts.getEventContext().remove("projectsForRegistration");
        Contexts.getEventContext().remove("registredConnectInstance");
    }

    public void validateLogin() {
        if (login == null || password == null) {
            facesMessages.add(FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get("label.empty.loginpassword"));
            loginValidated = false;
            flushEventCache();
            return;
        }
        List<ConnectProject> prjs = getProjectsAvailableForRegistration();
        if (prjs == null || prjs.size() == 0) {
            facesMessages.add(FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get("label.bad.loginpassword.or.noproject"));
            loginValidated = false;
            flushEventCache();
            return;
        }
        flushEventCache();
        loginValidated = true;
    }

    @Factory(value = "connectServerReachable", scope = ScopeType.EVENT)
    public boolean isConnectServerReachable() {
       return !ConnectStatusHolder.instance().getStatus().isConnectServerUnreachable();
    }

    public String refreshStatus() {
        ConnectStatusHolder.instance().getStatus(true);
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
        if (login == null) {
            return new ArrayList<ConnectProject>();
        }
        try {
            List<ConnectProject> projects = getService().getAvailableProjectsForRegistration(login,password);
            if (projects!=null && projects.size()>0) {
                Collections.sort(projects, new Comparator<ConnectProject>() {
                    @Override
                    public int compare(ConnectProject o1, ConnectProject o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
            }
            return projects;
        } catch (Exception e) {
            log.error("Error while getting remote project", e);
            return new ArrayList<ConnectProject>();
        }
    }

    public String resetRegister() {
        login=null;
        password=null;
        loginValidated = false;
        registredProject=null;
        instanceDescription = null;
        flushEventCache();
        return null;
    }

    public String register() {
        if (registredProject == null) {
            facesMessages.add(FacesMessage.SEVERITY_WARN,
            resourcesAccessor.getMessages().get("label.empty.project"));
            return null;
        }
        if (instanceDescription==null || instanceDescription.isEmpty()) {
            instanceDescription = login + "'s " + instanceType + " instance";
        }
        try {
            getService().remoteRegisterInstance(login, password,
                    registredProject,
                    NuxeoClientInstanceType.fromString(instanceType),
                    instanceDescription);
        } catch (Exception e) {
            facesMessages.add(FacesMessage.SEVERITY_ERROR,
                    resourcesAccessor.getMessages().get("label.connect.registrationError"));
            log.error("Error while registring instance", e);
        }

        flushEventCache();
        ConnectStatusHolder.instance().flush();
        return null;
    }

    public String getCTID() throws Exception {
        return TechnicalInstanceIdentifier.instance().getCTID();
    }

    public String localRegister() {
        try {
            getService().localRegisterInstance(CLID, instanceDescription);
        } catch (InvalidCLID e) {
            facesMessages.add(FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get("label.connect.wrongCLID"));
        } catch (IOException e) {
            facesMessages.add(FacesMessage.SEVERITY_ERROR,
                    resourcesAccessor.getMessages().get("label.connect.registrationError"));
            log.error("Error while registring instance locally", e);
        }

        flushEventCache();
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

        if (packageToUpload==null) {
            facesMessages.add(FacesMessage.SEVERITY_WARN,
            "label.connect.nofile");
            return;
        }

        PackageUpdateService pus = Framework.getLocalService(PackageUpdateService.class);

        File tmpFile = File.createTempFile("upload", "nxpkg");
        packageToUpload.transferTo(tmpFile);

        try {
            pus.addPackage(tmpFile);
        }
        catch (Exception e) {
            facesMessages.add(FacesMessage.SEVERITY_ERROR,
            resourcesAccessor.getMessages().get("label.connect.wrong.package" + ":" + e.getMessage()));
            return;
        } finally {
            tmpFile.delete();
            packageFileName=null;
            packageToUpload=null;
        }
    }
}
