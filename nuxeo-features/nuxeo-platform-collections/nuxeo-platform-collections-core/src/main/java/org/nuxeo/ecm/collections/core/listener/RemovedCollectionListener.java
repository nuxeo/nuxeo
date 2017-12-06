/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * Asynchronous event handler to update Collection of a removed CollectiomMember and CollectionMember of a Collection.
 *
 * @since 5.9.3
 */
public class RemovedCollectionListener implements PostCommitEventListener {

    private static final Log log = LogFactory.getLog(RemovedCollectionListener.class);

    @Override
    public void handleEvent(EventBundle bundle) {
        for (Event each : bundle) {
            final EventContext ctx = each.getContext();
            if (!(ctx instanceof DocumentEventContext)) {
                continue;
            }

            final String eventId = each.getName();
            if (!eventId.equals(DocumentEventTypes.DOCUMENT_REMOVED)) {
                continue;
            }

            onEvent(each);
        }
    }

    protected void onEvent(final Event event) {

        final DocumentEventContext docCxt = (DocumentEventContext) event.getContext();

        final DocumentModel doc = docCxt.getSourceDocument();

        final CollectionManager collectionManager = Framework.getService(CollectionManager.class);

        final boolean isCollectionRemoved = collectionManager.isCollection(doc);
        final boolean isCollectionMemberRemoved = collectionManager.isCollected(doc);

        if (isCollectionRemoved || isCollectionMemberRemoved) {
            if (isCollectionRemoved) {
                log.trace(String.format("Collection %s removed", doc.getId()));
                collectionManager.processRemovedCollection(doc);
            } else if (isCollectionMemberRemoved) {
                log.trace(String.format("CollectionMember %s removed", doc.getId()));
                collectionManager.processRemovedCollectionMember(doc);
            }
        }
    }

}
