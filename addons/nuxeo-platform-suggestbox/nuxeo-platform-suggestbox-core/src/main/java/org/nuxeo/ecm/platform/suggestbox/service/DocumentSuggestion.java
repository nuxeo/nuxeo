/*
 * (C) Copyright 2010-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.suggestbox.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;

/**
 * Suggest to navigate to a specific document.
 */
public class DocumentSuggestion extends Suggestion {

    private static final long serialVersionUID = 1L;

    private static final String PREFIX = "nxdoc";

    private static final String VIEW_ID = "view_documents";

    protected final DocumentLocation documentLocation;

    public DocumentSuggestion(String id, DocumentLocation documentLocation, String label,
            String iconURL) {
        super(id, CommonSuggestionTypes.DOCUMENT, label, iconURL);
        this.documentLocation = documentLocation;
    }

    public static Suggestion fromDocumentModel(DocumentModel doc)
            throws ClientException {
        TypeInfo typeInfo = doc.getAdapter(TypeInfo.class);
        String description = doc.getProperty("dc:description").getValue(
                String.class);
        String icon = null;
        if (doc.hasSchema("common")) {
            icon = (String) doc.getProperty("common", "icon");
        }
        if (StringUtils.isEmpty(icon)) {
            icon = typeInfo.getIcon();
        }
        return new DocumentSuggestion(doc.getId(), new DocumentLocationImpl(doc),
                doc.getTitle(), icon).withDescription(description);
    }

    public DocumentLocation getDocumentLocation() {
        return documentLocation;
    }

    @Override
    public String getObjectUrl() {
        if (documentLocation != null) {
            List<String> items = new ArrayList<String>();
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
