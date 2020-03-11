/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.collections.core.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * Event handler to restored the collection members of a restored version of a collection.
 *
 * @since 7.3
 */
public class RestoredCollectionListener implements EventListener {

    private static final Log log = LogFactory.getLog(RestoredCollectionListener.class);

    @Override
    public void handleEvent(Event event) {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }

        final String eventId = event.getName();

        final DocumentEventContext docCxt = (DocumentEventContext) event.getContext();
        final CollectionManager collectionManager = Framework.getService(CollectionManager.class);

        DocumentModel doc = null;
        DocumentModel version = null;
        if (eventId.equals(DocumentEventTypes.BEFORE_DOC_RESTORE)) {
            doc = docCxt.getSourceDocument();
            final String versionRefId = (String) docCxt.getProperties().get(
                    VersioningDocument.RESTORED_VERSION_UUID_KEY);
            version = docCxt.getCoreSession().getDocument(new IdRef(versionRefId));
            if (!collectionManager.isCollection(doc)) {
                return;
            }
        } else {
            return;
        }

        log.trace(String.format("Collection %s restored", doc.getId()));

        collectionManager.processRestoredCollection(doc, version);

    }

}
