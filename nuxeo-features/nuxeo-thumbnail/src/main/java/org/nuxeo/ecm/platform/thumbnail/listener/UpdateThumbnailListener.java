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
 * Vladimir Pasquier <vpasquier@nuxeo.com>
 * Laurent Doguin <ldoguin@nuxeo.com>
 * Nelson Silva <nsilva@nuxeo.com>
 */
package org.nuxeo.ecm.platform.thumbnail.listener;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.DeletedDocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;

/**
 * Thumbnail listener handling creation and update document event to store doc thumbnail preview (only for DocType File)
 *
 * @since 5.7
 */
public class UpdateThumbnailListener implements PostCommitEventListener {

    // @since 11.1
    private static final Logger log = LogManager.getLogger(UpdateThumbnailListener.class);

    public static final String THUMBNAIL_UPDATED = "thumbnailUpdated";

    // @since 11.5
    protected ThumbnailHelper thumbnailHelper = new ThumbnailHelper();

    protected void processDoc(CoreSession session, DocumentModel doc) {
        thumbnailHelper.createThumbnailIfNeeded(session, doc);
    }

    @Override
    public void handleEvent(EventBundle events) {
        if (!events.containsEventName(ThumbnailConstants.EventNames.scheduleThumbnailUpdate.name())) {
            return;
        }
        Set<String> processedDocs = new HashSet<>();
        for (Event event : events) {
            if (!ThumbnailConstants.EventNames.scheduleThumbnailUpdate.name().equals(event.getName())) {
                continue;
            }
            DocumentEventContext context = (DocumentEventContext) event.getContext();
            DocumentModel doc = context.getSourceDocument();
            if (Boolean.TRUE.equals(context.getProperty(ThumbnailConstants.DISABLE_THUMBNAIL_COMPUTATION))) {
                log.trace("Thumbnail computation is disabled for document {}", doc::getId);
                continue;
            }
            if (doc instanceof DeletedDocumentModel) {
                continue;
            }
            if (doc.isProxy()) {
                continue;
            }
            if (processedDocs.contains(doc.getId())) {
                continue;
            }
            thumbnailHelper.newTransaction();
            CoreSession repo = context.getCoreSession();
            processDoc(repo, doc);
            processedDocs.add(doc.getId());
        }
    }
}
