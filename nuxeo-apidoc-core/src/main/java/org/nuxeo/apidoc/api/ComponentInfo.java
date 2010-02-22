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

    public static final String TYPE_NAME ="NXComponent";

    public String getName();

    public BundleInfo getBundle();

    public Collection<ExtensionPointInfo> getExtensionPoints();

    public Collection<ExtensionInfo> getExtensions();

    public ExtensionPointInfo getExtensionPoint(String name);

    public String getDocumentation();

    public List<String> getServiceNames();

    public List<ServiceInfo> getServices();

    public String getComponentClass();

    public boolean isXmlPureComponent();

    public URL getXmlFileUrl();

    public String getXmlFileContent() throws IOException;

}