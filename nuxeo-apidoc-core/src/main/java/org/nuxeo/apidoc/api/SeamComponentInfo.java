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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.api;

import java.util.List;

public interface SeamComponentInfo extends NuxeoArtifact,
        Comparable<SeamComponentInfo> {

    String TYPE_NAME = "NXSeamComponent";

    String PROP_COMPONENT_NAME = "nxseam:componentName";

    String PROP_CLASS_NAME = "nxseam:className";

    String PROP_SCOPE = "nxseam:scope";

    String PROP_INTERFACES = "nxseam:interfaces";

    String PROP_PRECEDENCE = "nxseam:precedence";

    String getName();

    String getScope();

    String getPrecedence();

    String getClassName();

    List<String> getInterfaceNames();

}
