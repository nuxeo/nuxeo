/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 *
 */

package org.nuxeo.ecm.core.api.thumbnail;

import org.nuxeo.ecm.core.api.Blob;
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
    public Blob getThumbnail(CoreSession session) {
        ThumbnailService thumbnailService = Framework.getService(ThumbnailService.class);
        return thumbnailService.getThumbnail(doc, session);
    }

    public void save(CoreSession session) {
        session.saveDocument(doc);
    }

    public String getId() {
        return doc.getId();
    }

    @Override
    public Blob computeThumbnail(CoreSession session) {
        ThumbnailService thumbnailService = Framework.getService(ThumbnailService.class);
        return thumbnailService.computeThumbnail(doc, session);
    }

}
