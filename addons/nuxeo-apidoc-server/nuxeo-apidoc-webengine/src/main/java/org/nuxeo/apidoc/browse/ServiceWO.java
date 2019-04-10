/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
    public Object doGet() {
        ServiceInfo si = getServiceInfo();
        return getView("view").arg("service", si);
    }

    public ServiceInfo getServiceInfo() {
        return getSnapshotManager().getSnapshot(getDistributionId(), ctx.getCoreSession()).getService(nxArtifactId);
    }

    @Override
    public NuxeoArtifact getNxArtifact() {
        return getServiceInfo();
    }

}
