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

import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.ecm.webengine.model.WebObject;

@WebObject(type = "bundleGroup")
public class BundleGroupWO extends NuxeoArtifactWebObject {

    protected BundleGroup getTargetBundleGroup() {
        return getSnapshotManager().getSnapshot(getDistributionId(), ctx.getCoreSession()).getBundleGroup(nxArtifactId);
    }

    @Override
    public NuxeoArtifact getNxArtifact() {
        return getTargetBundleGroup();
    }

    public List<BundleWO> getBundles() {
        List<BundleWO> result = new ArrayList<>();

        BundleGroup group = getTargetBundleGroup();
        for (String bid : group.getBundleIds()) {
            result.add((BundleWO) ctx.newObject("bundle", bid));
        }
        return result;
    }

    public List<BundleGroupWO> getSubGroups() {
        List<BundleGroupWO> result = new ArrayList<>();

        BundleGroup group = getTargetBundleGroup();
        for (BundleGroup bg : group.getSubGroups()) {
            result.add((BundleGroupWO) ctx.newObject("bundleGroup", bg.getId()));
        }
        return result;
    }

}
