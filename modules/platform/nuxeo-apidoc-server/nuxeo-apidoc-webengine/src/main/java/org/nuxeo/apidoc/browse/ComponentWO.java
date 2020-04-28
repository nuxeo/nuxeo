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

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.ecm.webengine.model.WebObject;

@WebObject(type = "component")
public class ComponentWO extends NuxeoArtifactWebObject {

    @GET
    @Produces("text/xml")
    @Path("override")
    public Object override(@QueryParam("contributionId") String contribId) {
        ComponentInfo component = getTargetComponentInfo();
        ExtensionInfo contribution = getSnapshotManager().getSnapshot(getDistributionId(), ctx.getCoreSession())
                                                         .getContribution(contribId);
        return getView("override").arg("component", component).arg("contribution", contribution);
    }

    protected ComponentInfo getTargetComponentInfo() {
        return getSnapshotManager().getSnapshot(getDistributionId(), ctx.getCoreSession()).getComponent(nxArtifactId);
    }

    @Override
    public NuxeoArtifact getNxArtifact() {
        return getTargetComponentInfo();
    }

    public List<ServiceWO> getServices() {
        List<ServiceWO> result = new ArrayList<>();
        ComponentInfo ci = getTargetComponentInfo();
        for (ServiceInfo si : ci.getServices()) {
            result.add((ServiceWO) ctx.newObject("service", si.getId()));
        }
        return result;
    }

    public List<ExtensionPointWO> getExtensionPoints() {
        List<ExtensionPointWO> result = new ArrayList<>();
        ComponentInfo ci = getTargetComponentInfo();
        for (ExtensionPointInfo ei : ci.getExtensionPoints()) {
            result.add((ExtensionPointWO) ctx.newObject("extensionPoint", ei.getId()));
        }
        return result;
    }

    public List<ContributionWO> getContributions() {
        List<ContributionWO> result = new ArrayList<>();
        ComponentInfo ci = getTargetComponentInfo();
        for (ExtensionInfo ei : ci.getExtensions()) {
            result.add((ContributionWO) ctx.newObject("contribution", ei.getId()));
        }
        return result;
    }

}
