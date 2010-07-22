/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     mcedica
 */
package org.nuxeo.ecm.platform.management.probes.impl;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.management.administrativestatus.service.AdministrativeStatusService;
import org.nuxeo.ecm.platform.management.probes.Probe;
import org.nuxeo.ecm.platform.management.probes.ProbeStatus;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * Retrieves the administrative status of the server
 * 
 * @author Mariana Cedica
 */
public class AdministrativeStatusProbe implements Probe {

    ProbeStatus status;

    AdministrativeStatusService administrativeStatusService;

    public ProbeStatus getProbeStatus() {
        return status;
    }

    public void init(Object service) {
        // TODO Auto-generated method stub

    }

    public void runProbe(CoreSession session) throws ClientException {
        status = new ProbeStatus("For the server"
                + getAdministrativeStatusService().getServerInstanceName()
                + " the administrative status is: ");
        String serverStatus = getAdministrativeStatusService().getServerStatus(
                session);
        status.setStatus(status.getStatus() + serverStatus);

    }

    private AdministrativeStatusService getAdministrativeStatusService()
            throws ClientException {
        if (administrativeStatusService == null) {
            try {
                administrativeStatusService = Framework.getService(AdministrativeStatusService.class);
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
        return administrativeStatusService;
    }

}
