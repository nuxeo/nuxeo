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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
        return getSnapshotManager().getSnapshot(getDistributionId(),ctx.getCoreSession()).getBundle(nxArtifactId);
    }

    @Override
    public NuxeoArtifact getNxArtifact() {
        return getTargetBundleInfo();
    }

    protected class ComponentInfoSorter implements Comparator<ComponentInfo> {
        public int compare(ComponentInfo ci0, ComponentInfo ci1) {

            if (ci0.isXmlPureComponent() && ! ci1.isXmlPureComponent()) {
                return 1;
            }
            if (!ci0.isXmlPureComponent() && ci1.isXmlPureComponent()) {
                return -1;
            }

            return ci0.getId().compareTo(ci1.getId());
        }
    }

    public List<ComponentWO> getComponents() {
        List<ComponentWO> result = new ArrayList<ComponentWO>();
        BundleInfo bundle = getTargetBundleInfo();

        List<ComponentInfo> cis = new ArrayList<ComponentInfo>(bundle.getComponents());
        Collections.sort(cis, new ComponentInfoSorter());

        for (ComponentInfo ci : cis) {
            result.add((ComponentWO)ctx.newObject("component", ci.getId()));
        }
        return result;
    }

}
