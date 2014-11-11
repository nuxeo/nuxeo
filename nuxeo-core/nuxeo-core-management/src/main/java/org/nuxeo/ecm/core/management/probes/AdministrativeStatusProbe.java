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
package org.nuxeo.ecm.core.management.probes;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.management.api.AdministrativeStatus;
import org.nuxeo.ecm.core.management.api.AdministrativeStatusManager;
import org.nuxeo.ecm.core.management.api.Probe;
import org.nuxeo.ecm.core.management.api.ProbeStatus;
import org.nuxeo.runtime.api.Framework;

/**
 * Retrieves the administrative status of the server.
 *
 * @author Mariana Cedica
 */
public class AdministrativeStatusProbe implements Probe {

    @Override
    public ProbeStatus run() {
        AdministrativeStatusManager adm = Framework.getLocalService(AdministrativeStatusManager.class);
        AdministrativeStatus status = adm.getNuxeoInstanceStatus();

        Map<String, String> infos = new HashMap<String, String>();
        infos.put("server", status.getInstanceIdentifier());
        infos.put("host", Framework.getProperty(
                "org.nuxeo.runtime.server.host", "localhost"));
        infos.put("status", status.getState());
        if (!status.isActive()) {
            return ProbeStatus.newFailure(infos);
        }
        return ProbeStatus.newSuccess(infos);
    }

}
