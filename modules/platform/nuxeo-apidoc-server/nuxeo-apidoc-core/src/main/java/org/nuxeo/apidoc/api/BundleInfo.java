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
import java.util.Map;

import org.nuxeo.apidoc.documentation.ResourceDocumentationItem;

import com.fasterxml.jackson.annotation.JsonManagedReference;

public interface BundleInfo extends NuxeoArtifact {

    String TYPE_NAME = "NXBundle";

    String PROP_ARTIFACT_GROUP_ID = "nxbundle:artifactGroupId";

    String PROP_ARTIFACT_ID = "nxbundle:artifactId";

    String PROP_ARTIFACT_VERSION = "nxbundle:artifactVersion";

    String PROP_BUNDLE_ID = "nxbundle:bundleId";

    String PROP_JAR_NAME = "nxbundle:jarName";

    /**
     * @since 11.1
     */
    String RUNTIME_CONFIG_BUNDLE = "org.nuxeo.ecm.config";

    @JsonManagedReference("bundle")
    List<ComponentInfo> getComponents();

    String getFileName();

    String getBundleId();

    String[] getRequirements();

    String getManifest();

    String getLocation();

    String getGroupId();

    String getArtifactId();

    String getArtifactVersion();

    Map<String, ResourceDocumentationItem> getLiveDoc();

    Map<String, ResourceDocumentationItem> getParentLiveDoc();

}
