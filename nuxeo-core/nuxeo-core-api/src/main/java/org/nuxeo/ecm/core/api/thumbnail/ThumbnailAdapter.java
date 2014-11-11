/*
 * (C) Copyright 2006-2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 *
 */

package org.nuxeo.ecm.core.api.thumbnail;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;

/**
 * Thumbnail adapter getting thumbnail blob from thumbnail registered factories
 *
 * @since 5.7
 */
public class ThumbnailAdapter implements Thumbnail {

    protected final DocumentModel doc;

    public ThumbnailAdapter(DocumentModel doc) {
        this.doc = doc;
    }

    @Override
    public Blob getThumbnail(CoreSession session) throws ClientException {
        ThumbnailService thumbnailService = Framework.getLocalService(ThumbnailService.class);
        return thumbnailService.getThumbnail(doc, session);
    }

    public void save(CoreSession session) throws ClientException {
        session.saveDocument(doc);
    }

    public String getId() {
        return doc.getId();
    }

    @Override
    public Blob computeThumbnail(CoreSession session) throws ClientException {
        ThumbnailService thumbnailService = Framework.getLocalService(ThumbnailService.class);
        return thumbnailService.computeThumbnail(doc, session);
    }

}
