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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.WebObject;

@WebObject(type = "contribution")
public class ContributionWO extends NuxeoArtifactWebObject {

    @Override
    @GET
    @Produces("text/html")
    @Path("introspection")
    public Object doGet() throws Exception {
        ExtensionInfo ei = getTargetExtensionInfo();
        return getView("view").arg("contribution", ei);
    }

    public ExtensionInfo getTargetExtensionInfo() {
        return getSnapshotManager().getSnapshot(getDistributionId(),
                ctx.getCoreSession()).getContribution(nxArtifactId);
    }

    @Override
    public NuxeoArtifact getNxArtifact() {
        return getTargetExtensionInfo();
    }

    @POST
    @Produces("text/xml")
    @Path("override")
    public Object generateOverride() throws Exception {

        ExtensionInfo ei = getTargetExtensionInfo();
        String epid = ei.getExtensionPoint();
        ExtensionPointInfo ep = getSnapshotManager().getSnapshot(
                getDistributionId(), ctx.getCoreSession()).getExtensionPoint(
                epid);

        FormData formData = ctx.getForm();
        Map<String, String[]> fields = formData.getFormFields();
        List<String> selectedContribs = new ArrayList<String>(fields.keySet());
        return getView("override").arg("contribution", ei).arg(
                "selectedContribs", selectedContribs).arg("ep", ep);
    }

}
