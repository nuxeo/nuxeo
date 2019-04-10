/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Bogdan Stefanescu
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.api;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

public interface ComponentInfo extends NuxeoArtifact {

    String TYPE_NAME = "NXComponent";

    String PROP_COMPONENT_ID = "nxcomponent:componentId";

    String PROP_COMPONENT_NAME = "nxcomponent:componentName";

    String PROP_COMPONENT_CLASS = "nxcomponent:componentClass";

    String PROP_BUILT_IN_DOC = "nxcomponent:builtInDocumentation";

    String PROP_IS_XML = "nxcomponent:isXML";

    String PROP_SERVICES = "nxcomponent:services";

    String getName();

    BundleInfo getBundle();

    Collection<ExtensionPointInfo> getExtensionPoints();

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
