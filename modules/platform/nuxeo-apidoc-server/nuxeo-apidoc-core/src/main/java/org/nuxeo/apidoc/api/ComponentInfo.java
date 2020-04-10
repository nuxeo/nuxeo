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

import java.io.IOException;
import java.net.URL;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

public interface ComponentInfo extends NuxeoArtifact {

    String TYPE_NAME = "NXComponent";

    String PROP_COMPONENT_ID = "nxcomponent:componentId";

    String PROP_COMPONENT_NAME = "nxcomponent:componentName";

    String PROP_COMPONENT_CLASS = "nxcomponent:componentClass";

    String PROP_BUILT_IN_DOC = "nxcomponent:builtInDocumentation";

    String PROP_IS_XML = "nxcomponent:isXML";

    String PROP_SERVICES = "nxcomponent:services";

    @Override
    @JsonIgnore
    String getId();

    String getName();

    @JsonBackReference("bundle")
    BundleInfo getBundle();

    @JsonManagedReference("extensionpoint")
    List<ExtensionPointInfo> getExtensionPoints();

    @JsonManagedReference("extension")
    List<ExtensionInfo> getExtensions();

    String getDocumentation();

    String getDocumentationHtml();

    @JsonIgnore
    List<String> getServiceNames();

    @JsonManagedReference("service")
    List<ServiceInfo> getServices();

    String getComponentClass();

    boolean isXmlPureComponent();

    @JsonIgnore
    URL getXmlFileUrl();

    String getXmlFileName();

    String getXmlFileContent() throws IOException;

}
