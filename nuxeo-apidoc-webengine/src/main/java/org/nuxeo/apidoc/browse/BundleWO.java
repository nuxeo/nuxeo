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

import java.util.Collection;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;

import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.webengine.model.WebObject;

/**
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */
@WebObject(type = "bundle")
public class BundleWO extends NuxeoArtifactWebObject {

    @GET
    @Produces("text/html")
    public Object doGet() throws Exception {
        BundleInfo bi = getTargetBundleInfo();
        Collection<ComponentInfo> ci = bi.getComponents();
        return getView("view").arg("bundle", bi).arg("components", ci);
    }

    public BundleInfo getTargetBundleInfo() {
        return SnapshotManager.getSnapshot(getDistributionId(),ctx.getCoreSession()).getBundle(nxArtifactId);
    }

    @Override
    protected NuxeoArtifact getNxArtifact() {
        return getTargetBundleInfo();
    }


}
