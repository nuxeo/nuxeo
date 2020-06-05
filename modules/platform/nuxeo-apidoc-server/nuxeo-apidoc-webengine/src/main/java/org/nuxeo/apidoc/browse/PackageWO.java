/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.browse;

import javax.ws.rs.Produces;

import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.PackageInfo;
import org.nuxeo.ecm.webengine.model.Template;
import org.nuxeo.ecm.webengine.model.WebObject;

/**
 * Web Object for {@link PackageInfo} display.
 *
 * @since 11.1
 */
@WebObject(type = PackageWO.TYPE)
public class PackageWO extends NuxeoArtifactWebObject {

    public static final String TYPE = "package";

    @Override
    public NuxeoArtifact getNxArtifact() {
        return getTargetPackageInfo();
    }

    protected PackageInfo getTargetPackageInfo() {
        return getSnapshotManager().getSnapshot(getDistributionId(), ctx.getCoreSession()).getPackage(nxArtifactId);
    }

    @Produces("text/html")
    @Override
    public Object doViewDefault() {
        Template t = (Template) super.doViewDefault();
        String marketplaceURL = PackageInfo.getMarketplaceURL(getTargetPackageInfo(), true);
        t.arg("marketplaceURL", marketplaceURL);
        return t;
    }

    @Override
    public String getSearchCriterion() {
        return "'" + super.getSearchCriterion() + "' Package";
    }

}
