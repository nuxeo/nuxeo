/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.ecm.admin.monitoring;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.management.api.AdministrativeStatus;
import org.nuxeo.ecm.core.management.api.AdministrativeStatusManager;
import org.nuxeo.ecm.core.management.api.GlobalAdministrativeStatusManager;
import org.nuxeo.ecm.core.management.api.ProbeInfo;
import org.nuxeo.ecm.core.management.api.ProbeManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Seam Bean that wraps {@link AdministrativeStatusManager} and {@link ProbeManager} services to provide a JSF UI.
 *
 * @author tiry
 */
@Name("monitoringAction")
@Scope(ScopeType.PAGE)
public class MonitoringActionBean implements Serializable {

    public static final String NUXEO_SERVER_IS_ACTIVE = "nuxeoServiceIsActive";

    public static final String NUXEO_SERVER_MESSAGE = "org.nuxeo.ecm.deactivatedServerMessage";

    protected static final Log log = LogFactory.getLog(MonitoringActionBean.class);

    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected NuxeoPrincipal currentNuxeoPrincipal;

    @RequestParameter("serviceIdentifier")
    protected String statusIdentifierToEdit;

    @RequestParameter("probeName")
    protected String probeName;

    protected String newStatusMessage;

    protected String newStatusValue;

    protected AdministrativeStatus editableStatus;

    public AdministrativeStatus getEditableStatus() {
        return editableStatus;
    }

    protected AdministrativeStatusManager getStatusManager() {
        return Framework.getService(AdministrativeStatusManager.class);
    }

    public List<String> listNuxeoInstances() {
        return Framework.getService(GlobalAdministrativeStatusManager.class).listInstanceIds();
    }

    @Factory(value = "isMultiServerInstallation", scope = ScopeType.EVENT)
    public boolean isMultiServerInstallation() {
        return listNuxeoInstances().size() > 1;
    }

    public List<AdministrativeStatus> getAdministrativeStatuses() {
        List<AdministrativeStatus> statuses = getStatusManager().getAllStatuses();
        for (AdministrativeStatus status : statuses) {
            log.info("Status : " + status.getLabel() + "=>" + status.getState());
        }
        return statuses;
    }

    public void editStatus() {
        editableStatus = getStatusManager().getStatus(statusIdentifierToEdit);
        newStatusMessage = editableStatus.getMessage();
        if (editableStatus.isActive()) {
            newStatusValue = AdministrativeStatus.PASSIVE;
        } else {
            newStatusValue = AdministrativeStatus.ACTIVE;
        }
    }

    public void saveStatus() {
        if (editableStatus != null) {
            String serviceId = editableStatus.getServiceIdentifier();
            changeStatus(serviceId, newStatusMessage, newStatusValue);
            resetEdit();
        }
    }

    public void cancelEdit() {
        resetEdit();
    }

    public void resetEdit() {
        editableStatus = null;
        newStatusMessage = null;
        newStatusValue = null;
    }

    public void changeStatus(String serviceId, String message, String state) {
        getStatusManager().setStatus(serviceId, state, message, currentNuxeoPrincipal.getName());
    }

    public List<ProbeInfo> getProbeInfos() {
        List<ProbeInfo> infos = new ArrayList<>();
        ProbeManager pm = Framework.getService(ProbeManager.class);
        infos.addAll(pm.getAllProbeInfos());
        return infos;
    }

    public void runProbe() {
        ProbeManager pm = Framework.getService(ProbeManager.class);
        pm.runProbe(probeName);
    }

    public void runAllProbes() {
        ProbeManager pm = Framework.getService(ProbeManager.class);
        pm.runAllProbes();
    }

    public String getNewStatusMessage() {
        return newStatusMessage;
    }

    public void setNewStatusMessage(String newStatusMessage) {
        this.newStatusMessage = newStatusMessage;
    }

    public String getNewStatusValue() {
        return newStatusValue;
    }

    public void setNewStatusValue(String newStatusValue) {
        this.newStatusValue = newStatusValue;
    }

}
