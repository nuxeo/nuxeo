/*
 * (C) Copyright 2010-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.suggestbox.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;

/**
 * Suggest to navigate to a specific document.
 */
public class DocumentSuggestion extends Suggestion {

    private static final long serialVersionUID = 1L;

    private static final String PREFIX = "nxdoc";

    private static final String VIEW_ID = "view_documents";

    protected final DocumentLocation documentLocation;

    public DocumentSuggestion(String id, DocumentLocation documentLocation, String label, String iconURL) {
        super(id, CommonSuggestionTypes.DOCUMENT, label, iconURL);
        this.documentLocation = documentLocation;
    }

    public static Suggestion fromDocumentModel(DocumentModel doc) {
        TypeInfo typeInfo = doc.getAdapter(TypeInfo.class);
        String description = doc.getProperty("dc:description").getValue(String.class);
        String icon = null;
        if (doc.hasSchema("common")) {
            icon = (String) doc.getProperty("common", "icon");
        }
        if (StringUtils.isEmpty(icon)) {
            icon = typeInfo.getIcon();
        }
        String thumbnailURL = String.format("api/v1/id/%s/@rendition/thumbnail", doc.getId());
        @SuppressWarnings("unchecked")
        Map<String, List<String>> highlights = (Map<String, List<String>>) doc.getContextData(
                PageProvider.HIGHLIGHT_CTX_DATA);
        return new DocumentSuggestion(doc.getId(), new DocumentLocationImpl(doc), doc.getTitle(), icon).withDescription(
                description).withThumbnailURL(thumbnailURL).withHighlights(highlights);
    }

    public DocumentLocation getDocumentLocation() {
        return documentLocation;
    }

    @Override
    public String getObjectUrl() {
        if (documentLocation != null) {
            List<String> items = new ArrayList<>();
            items.add(PREFIX);
            items.add(documentLocation.getServerName());
            IdRef docRef = documentLocation.getIdRef();
            if (docRef == null) {
                return null;
            }
            items.add(docRef.toString());
            items.add(VIEW_ID);

            String uri = StringUtils.join(items, "/");
            return uri;
        }
        return null;
    }
}
