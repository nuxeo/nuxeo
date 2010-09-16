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
 *
 */
@Name("monitoringAction")
@Scope(ScopeType.EVENT)
public class MonitoringActionBean implements Serializable {

    protected static final Log log = LogFactory.getLog(MonitoringActionBean.class);

    private static final long serialVersionUID = 1L;

    public static final String NUXEO_SERVER_IS_ACTIVE = "nuxeoServiceIsActive";

    public static final String NUXEO_SERVER_MESSAGE = "org.nuxeo.ecm.deactivatedServerMessage";

    @In(create=true)
    protected NuxeoPrincipal currentNuxeoPrincipal;

    @RequestParameter("serviceIdentifier")
    protected String newStatusIdentifier;

    @RequestParameter("probeName")
    protected String probeName;

    protected String newStatusMessage;

    public String getNewStatusMessage() {
        return newStatusMessage;
    }

    public void setNewStatusMessage(String newStatusMessage) {
        this.newStatusMessage = newStatusMessage;
    }

    protected AdministrativeStatusManager getStatusManager() {
        return Framework.getLocalService(AdministrativeStatusManager.class);
    }

    public List<String> listNuxeoInstances() {
        return Framework.getLocalService(GlobalAdministrativeStatusManager.class).listInstanceIds();
    }

    @Factory(value="isMultiServerInstallation", scope=ScopeType.EVENT)
    public boolean isMultiServerInstallation() {
        return listNuxeoInstances().size()>1;
    }

    public List<AdministrativeStatus> getAdministrativeStatuses() {
        List<AdministrativeStatus> statuses = getStatusManager().getAllStatuses();
        for (AdministrativeStatus status : statuses) {
            log.info("Status : " + status.getLabel() + "=>" + status.getState());
        }
        return statuses;
    }

    public void activateService() {
        changeStatus(newStatusIdentifier, newStatusMessage, AdministrativeStatus.ACTIVE);
    }

    public void passivateService() {
        changeStatus(newStatusIdentifier, newStatusMessage, AdministrativeStatus.PASSIVE);
    }

    public void changeStatus(String serviceId, String message, String state) {
        getStatusManager().setStatus(serviceId, state, message, currentNuxeoPrincipal.getName());
        newStatusMessage=null;
    }

    public List<ProbeInfo> getProbeInfos() {
        List<ProbeInfo> infos = new ArrayList<ProbeInfo>();
        ProbeManager pm = Framework.getLocalService(ProbeManager.class);
        infos.addAll(pm.getAllProbeInfos());
        return infos;
    }

    public void runProbe() {
        ProbeManager pm = Framework.getLocalService(ProbeManager.class);
        pm.runProbe(probeName);
    }

    public void runAllProbes() {
        ProbeManager pm = Framework.getLocalService(ProbeManager.class);
        pm.runAllProbes();
    }

}
