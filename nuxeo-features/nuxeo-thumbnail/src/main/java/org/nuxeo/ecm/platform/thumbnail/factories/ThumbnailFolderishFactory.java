/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 *
 */
package org.nuxeo.ecm.platform.thumbnail.factories;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailAdapter;

/**
 * Default thumbnail factory for all folderish documents
 *
 * @since 5.7
 */
public class ThumbnailFolderishFactory extends ThumbnailDocumentFactory {

    /**
     * @since 7.10-HF15
     */
    public static final String FIRST_CHILD_QUERY = "SELECT * FROM Document WHERE ecm:parentId = '%s' AND "
            + "ecm:currentLifeCycleState <> 'deleted' AND ecm:mixinType <> 'HiddenInNavigation' AND "
            + "ecm:mixinType <> 'Folderish'";

    @Override
    public Blob getThumbnail(DocumentModel doc, CoreSession session) {
        if (!doc.isFolder()) {
            throw new NuxeoException("Document is not folderish");
        }

        String query = String.format(FIRST_CHILD_QUERY, doc.getId());
        DocumentModelList children = session.query(query, 1);
        if (!children.isEmpty()) {
            return children.get(0).getAdapter(ThumbnailAdapter.class).getThumbnail(session);
        }
        return getDefaultThumbnail(doc);
    }

    @Override
    public Blob computeThumbnail(DocumentModel doc, CoreSession session) {
        return null;
    }
}
