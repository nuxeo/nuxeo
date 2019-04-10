/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.apidoc.browse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.ecm.webengine.model.WebObject;

@WebObject(type = "service")
public class ServiceWO extends NuxeoArtifactWebObject {

    @Override
    @GET
    @Produces("text/html")
    @Path("introspection")
    public Object doGet() throws Exception {
        ServiceInfo si = getServiceInfo();
        return getView("view").arg("service", si);
    }

    public ServiceInfo getServiceInfo() {
        return getSnapshotManager().getSnapshot(getDistributionId(),
                ctx.getCoreSession()).getService(nxArtifactId);
    }

    @Override
    public NuxeoArtifact getNxArtifact() {
        return getServiceInfo();
    }

}
