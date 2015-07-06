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
package org.nuxeo.apidoc.documentation;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.api.DocumentationItem;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.ecm.core.api.CoreSession;

public interface DocumentationService {

    DocumentationItem createDocumentationItem(CoreSession session, NuxeoArtifact item, String title, String content,
            String type, List<String> applicableVersions, boolean approved, String renderingType);

    DocumentationItem updateDocumentationItem(CoreSession session, DocumentationItem docItem);

    void deleteDocumentationItem(CoreSession session, String uuid);

    List<DocumentationItem> findDocumentItems(CoreSession session, NuxeoArtifact nxItem);

    List<DocumentationItem> findDocumentationItemVariants(CoreSession session, DocumentationItem item);

    Map<String, String> getCategories();

    List<String> getCategoryKeys();

    void exportDocumentation(CoreSession session, OutputStream out);

    void importDocumentation(CoreSession session, InputStream is);

    String getDocumentationStats(CoreSession session);

    Map<String, List<DocumentationItem>> listDocumentationItems(CoreSession session, String category, String targetType);

    Map<String, DocumentationItem> getAvailableDescriptions(CoreSession session, String targetType);

}
