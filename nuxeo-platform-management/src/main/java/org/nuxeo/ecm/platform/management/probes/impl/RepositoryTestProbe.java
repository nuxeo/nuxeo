/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.management.probes.Probe;
import org.nuxeo.ecm.platform.management.probes.ProbeStatus;

public class RepositoryTestProbe implements Probe {

    private static final String queryString = "Select * from Document where ecm:path STARTSWITH '/'";

    ProbeStatus status;

    public void init(Object service) {
        // TODO Auto-generated method stub

    }

    public void runProbe(CoreSession session) throws ClientException {
        status = new ProbeStatus("Running " + queryString + ":");
        DocumentModelList list = session.query(queryString);
        for (DocumentModel documentModel : list) {
            status.setStatus(status.getStatus() + " "
                    + documentModel.getTitle());
        }

    }

    public ProbeStatus getProbeStatus() {
        return status;
    }

}
