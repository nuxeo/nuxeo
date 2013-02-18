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

import java.io.File;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailAdapter;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailFactory;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;

/**
 * Default thumbnail factory for all folderish documents
 * 
 * @since 5.7
 */
public class ThumbnailFolderishFactory implements ThumbnailFactory {

    @Override
    public Blob getThumbnail(DocumentModel doc, CoreSession session)
            throws ClientException {
        if (!doc.isFolder()) {
            throw new ClientException("Document is not folderish");
        }
        DocumentRef docRef = doc.getRef();
        if (session.hasChildren(docRef)) {
            DocumentModel child = session.getChildren(docRef).get(0);
            if (!child.isFolder()) {
                return session.getChildren(docRef).get(0).getAdapter(
                        ThumbnailAdapter.class).getThumbnail(session);
            }
        }
        TypeInfo docType = doc.getAdapter(TypeInfo.class);
        return new FileBlob(FileUtils.getResourceFileFromContext("nuxeo.war"
                + File.separator + docType.getBigIcon()));
    }

    @Override
    public Blob computeThumbnail(DocumentModel doc, CoreSession session) {
        return null;
    }
}
