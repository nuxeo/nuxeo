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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.collections.core.adapter.CollectionMember;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Event handler to asynchronously update the list of collection member of a
 * duplicated collection.
 *
 * @since 5.9.3
 */
public class DuplicatedCollectionListener implements EventListener {

    private static final Log log = LogFactory.getLog(DuplicatedCollectionListener.class);

    @Override
    public void handleEvent(Event event) throws ClientException {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }

        final String eventId = event.getName();

        if (!eventId.equals(DocumentEventTypes.DOCUMENT_DUPLICATED)) {
            return;
        }
        final DocumentEventContext docCxt = (DocumentEventContext) event.getContext();

        final DocumentModel doc = docCxt.getSourceDocument();

        final CollectionManager collectionManager = Framework.getLocalService(CollectionManager.class);

        if (collectionManager.isCollection(doc)) {

            final CoreSession session = docCxt.getCoreSession();

            processUpdate(doc, session);

        }
    }

    private void processUpdate(final DocumentModel doc,
            final CoreSession session) throws ClientException {
        List<DocumentModel> results = null;

        // Bash style
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        results = getNextResults(doc, session);

        do {
            boolean succeed = false;

            try {

                for (DocumentModel d : results) {
                    log.trace(String.format("Updating CollectionMember %s",
                            d.getTitle()));
                    CollectionMember collectionMember = d.getAdapter(CollectionMember.class);
                    collectionMember.addToCollection(doc.getId());
                    session.saveDocument(d);
                }
                succeed = true;
            } finally {
                if (succeed) {
                    TransactionHelper.commitOrRollbackTransaction();
                    TransactionHelper.startTransaction();
                    results = getNextResults(doc, session);
                }
            }
        } while (results != null && !results.isEmpty());
    }

    private List<DocumentModel> getNextResults(final DocumentModel doc, CoreSession session)
            throws ClientException {
        List<DocumentModel> results;
        Object[] parameters = new Object[1];
        parameters[0] = doc.getId();

        String query = NXQLQueryBuilder.getQuery(
                CollectionAsynchrnonousQuery.QUERY_FOR_COLLECTION_DUPLICATED,
                parameters, true, false);

        results = session.query(query, null,
                CollectionAsynchrnonousQuery.MAX_RESULT, 0,
                CollectionAsynchrnonousQuery.MAX_RESULT);
        return results;
    }

}
