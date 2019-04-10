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

import java.util.Collection;

public interface ExtensionPointInfo extends NuxeoArtifact {

    String TYPE_NAME = "NXExtensionPoint";

    String PROP_NAME = "nxextensionpoint:name";

    String PROP_EP_ID = "nxextensionpoint:epId";

    String PROP_DOC = "nxextensionpoint:documentation";

    /** misnamed in schema */
    String PROP_DESCRIPTORS = "nxextensionpoint:extensionPoint";

    ComponentInfo getComponent();

    String getComponentId();

    String getName();

    String[] getDescriptors();

    Collection<ExtensionInfo> getExtensions();

    String getDocumentation();

    String getDocumentationHtml();

    String getLabel();

}
