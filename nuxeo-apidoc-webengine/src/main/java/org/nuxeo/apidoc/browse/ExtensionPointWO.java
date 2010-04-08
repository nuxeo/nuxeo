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

package org.nuxeo.apidoc.browse;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;

import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.ecm.webengine.model.WebObject;

/**
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */
@WebObject(type = "extensionPoint")
public class ExtensionPointWO extends NuxeoArtifactWebObject {


    @GET
    @Produces("text/html")
    public Object doGet() throws Exception {
        ExtensionPointInfo epi = getTargetExtensionPointInfo();
        return getView("view").arg("extensionPoint", epi);
    }

    public ExtensionPointInfo getTargetExtensionPointInfo() {
        return getSnapshotManager().getSnapshot(getDistributionId(),ctx.getCoreSession()).getExtensionPoint(nxArtifactId);
    }
    @Override
    public NuxeoArtifact getNxArtifact() {
        return getTargetExtensionPointInfo();
    }


}
