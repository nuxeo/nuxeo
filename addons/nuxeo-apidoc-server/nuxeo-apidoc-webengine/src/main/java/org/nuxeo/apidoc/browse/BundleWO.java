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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.ecm.webengine.model.WebObject;

@WebObject(type = "bundle")
public class BundleWO extends NuxeoArtifactWebObject {

    @Override
    @GET
    @Produces("text/html")
    @Path("introspection")
    public Object doGet() {
        BundleInfo bi = getTargetBundleInfo();
        Collection<ComponentInfo> ci = bi.getComponents();
        return getView("view").arg("bundle", bi).arg("components", ci);
    }

    public BundleInfo getTargetBundleInfo() {
        return getSnapshotManager().getSnapshot(getDistributionId(), ctx.getCoreSession()).getBundle(nxArtifactId);
    }

    @Override
    public NuxeoArtifact getNxArtifact() {
        return getTargetBundleInfo();
    }

    protected class ComponentInfoSorter implements Comparator<ComponentInfo> {
        @Override
        public int compare(ComponentInfo ci0, ComponentInfo ci1) {

            if (ci0.isXmlPureComponent() && !ci1.isXmlPureComponent()) {
                return 1;
            }
            if (!ci0.isXmlPureComponent() && ci1.isXmlPureComponent()) {
                return -1;
            }

            return ci0.getId().compareTo(ci1.getId());
        }
    }

    public List<ComponentWO> getComponents() {
        List<ComponentWO> result = new ArrayList<>();
        BundleInfo bundle = getTargetBundleInfo();

        List<ComponentInfo> cis = new ArrayList<>(bundle.getComponents());
        Collections.sort(cis, new ComponentInfoSorter());

        for (ComponentInfo ci : cis) {
            result.add((ComponentWO) ctx.newObject("component", ci.getId()));
        }
        return result;
    }

    @Override
    public String getSearchCriterion() {
        return "'" + super.getSearchCriterion() + "' Bundle";
    }
}
