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

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.ecm.webengine.model.WebObject;

@WebObject(type = "extensionPoint")
public class ExtensionPointWO extends NuxeoArtifactWebObject {

    protected ExtensionPointInfo getTargetExtensionPointInfo() {
        return getSnapshotManager().getSnapshot(getDistributionId(), ctx.getCoreSession())
                                   .getExtensionPoint(nxArtifactId);
    }

    @Override
    public NuxeoArtifact getNxArtifact() {
        return getTargetExtensionPointInfo();
    }

    @Override
    public String getSearchCriterion() {
        String[] split = getNxArtifactId().split("--");
        if (split.length == 2) {
            return String.format("'%s' %s", split[0], split[1]);
        } else if (split.length > 1) {
            return StringUtils.join(split, " ");
        }
        return getNxArtifactId();
    }
}
