/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.apidoc.api;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */
public interface ComponentInfo extends NuxeoArtifact {

    static final String TYPE_NAME ="NXComponent";

    String getName();

    BundleInfo getBundle();

    Collection<ExtensionPointInfo> getExtensionPoints();

    Collection<ExtensionInfo> getExtensions();

    ExtensionPointInfo getExtensionPoint(String name);

    String getDocumentation();

    List<String> getServiceNames();

    List<ServiceInfo> getServices();

    String getComponentClass();

    boolean isXmlPureComponent();

    URL getXmlFileUrl();

    String getXmlFileName();

    String getXmlFileContent() throws IOException;

}
