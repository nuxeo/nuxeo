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
 *     Bogdan Stefanescu
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.api;

import java.util.List;

import org.nuxeo.apidoc.documentation.ContributionItem;
import org.nuxeo.runtime.model.ComponentName;

import com.fasterxml.jackson.annotation.JsonBackReference;

public interface ExtensionInfo extends NuxeoArtifact {

    String TYPE_NAME = "NXContribution";

    String PROP_CONTRIB_ID = "nxcontribution:contribId";

    String PROP_DOC = "nxcontribution:documentation";

    String PROP_EXTENSION_POINT = "nxcontribution:extensionPoint";

    String PROP_TARGET_COMPONENT_NAME = "nxcontribution:targetComponentName";

    /**
     * Returns a key combining the target component name and the extension point name.
     */
    String getExtensionPoint();

    String getDocumentation();

    String getDocumentationHtml();

    ComponentName getTargetComponentName();

    String getXml();

    List<ContributionItem> getContributionItems();

    @JsonBackReference("extension")
    ComponentInfo getComponent();

}
