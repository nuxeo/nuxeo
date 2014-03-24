/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.collections.core.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * Asynchronous event handler to update Collection of a removed CollectiomMember
 * and CollectionMember of a Collection.
 *
 * @since 5.9.3
 */
public class RemovedCollectionListener implements PostCommitEventListener {

    private static final Log log = LogFactory.getLog(RemovedCollectionListener.class);

    @Override
    public void handleEvent(EventBundle bundle) throws ClientException {
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

    protected void onEvent(final Event event) throws ClientException {

        final DocumentEventContext docCxt = (DocumentEventContext) event.getContext();

        final DocumentModel doc = docCxt.getSourceDocument();

        final CollectionManager collectionManager = Framework.getLocalService(CollectionManager.class);

        final boolean isCollectionRemoved = collectionManager.isCollection(doc);
        final boolean isCollectionMemberRemoved = collectionManager.isCollected(doc);

        if (isCollectionRemoved || isCollectionMemberRemoved) {
            if (isCollectionRemoved) {
                log.trace(String.format("Collection %s removed", doc.getId()));
                collectionManager.processRemovedCollection(doc);
            } else if (isCollectionMemberRemoved) {
                log.trace(String.format("CollectionMember %s removed",
                        doc.getId()));
                collectionManager.processRemovedCollectionMember(doc);
            }
        }
    }

}
