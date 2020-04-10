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

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

public interface ExtensionPointInfo extends NuxeoArtifact {

    String TYPE_NAME = "NXExtensionPoint";

    String PROP_NAME = "nxextensionpoint:name";

    String PROP_EP_ID = "nxextensionpoint:epId";

    String PROP_DOC = "nxextensionpoint:documentation";

    /** misnamed in schema */
    String PROP_DESCRIPTORS = "nxextensionpoint:extensionPoint";

    @JsonBackReference("extensionpoint")
    ComponentInfo getComponent();

    String getComponentId();

    String getName();

    String[] getDescriptors();

    @JsonIgnore
    List<ExtensionInfo> getExtensions();

    String getDocumentation();

    String getDocumentationHtml();

    String getLabel();

}
