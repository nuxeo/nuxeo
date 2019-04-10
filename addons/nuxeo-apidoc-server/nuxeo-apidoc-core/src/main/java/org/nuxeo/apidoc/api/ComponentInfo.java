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
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class)
public interface ComponentInfo extends NuxeoArtifact {

    String TYPE_NAME = "NXComponent";

    String PROP_COMPONENT_ID = "nxcomponent:componentId";

    String PROP_COMPONENT_NAME = "nxcomponent:componentName";

    String PROP_COMPONENT_CLASS = "nxcomponent:componentClass";

    String PROP_BUILT_IN_DOC = "nxcomponent:builtInDocumentation";

    String PROP_IS_XML = "nxcomponent:isXML";

    String PROP_SERVICES = "nxcomponent:services";

    String getName();

    @JsonManagedReference("component")
    BundleInfo getBundle();

    @JsonIgnore
    Collection<ExtensionPointInfo> getExtensionPoints();

    @JsonIgnore
    Collection<ExtensionInfo> getExtensions();

    ExtensionPointInfo getExtensionPoint(String name);

    String getDocumentation();

    String getDocumentationHtml();

    List<String> getServiceNames();

    List<ServiceInfo> getServices();

    String getComponentClass();

    boolean isXmlPureComponent();

    URL getXmlFileUrl();

    String getXmlFileName();

    String getXmlFileContent() throws IOException;

}
