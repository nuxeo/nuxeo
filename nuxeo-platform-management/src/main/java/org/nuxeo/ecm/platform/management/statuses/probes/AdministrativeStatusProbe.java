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
package org.nuxeo.ecm.platform.management.statuses.probes;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.management.statuses.AdministrativeStatus;
import org.nuxeo.ecm.platform.management.statuses.Probe;
import org.nuxeo.ecm.platform.management.statuses.ProbeStatus;
import org.nuxeo.runtime.api.Framework;

/**
 * Retrieves the administrative status of the server.
 * 
 * @author Mariana Cedica
 */
public class AdministrativeStatusProbe implements Probe {

    ProbeStatus status;

    AdministrativeStatus administrativeStatus;

    public ProbeStatus getProbeStatus() {
        return status;
    }

    public void init(Object service) {
        
    }

    public void runProbe(CoreSession session) throws ClientException {
        status = new ProbeStatus("For the server"
                + getAdministrativeStatusService().getServerInstanceName()
                + " the administrative status is: ");
        String serverStatus = getAdministrativeStatusService().getValue();
        status.setStatus(status.getStatus() + serverStatus);

    }

    private AdministrativeStatus getAdministrativeStatusService()
            throws ClientException {
        if (administrativeStatus == null) {
            try {
                administrativeStatus = Framework.getService(AdministrativeStatus.class);
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
        return administrativeStatus;
    }

}
