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
