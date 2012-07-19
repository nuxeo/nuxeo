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

import java.util.List;

import org.nuxeo.apidoc.documentation.ContributionItem;
import org.nuxeo.runtime.model.ComponentName;

public interface ExtensionInfo extends NuxeoArtifact {

    String TYPE_NAME = "NXContribution";

    String PROP_CONTRIB_ID = "nxcontribution:contribId";

    String PROP_DOC = "nxcontribution:documentation";

    String PROP_EXTENSION_POINT = "nxcontribution:extensionPoint";

    String PROP_TARGET_COMPONENT_NAME = "nxcontribution:targetComponentName";

    String getExtensionPoint();

    String getDocumentation();

    String getDocumentationHtml();

    ComponentName getTargetComponentName();

    String getXml();

    List<ContributionItem> getContributionItems();

}
