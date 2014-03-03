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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.collections.core.adapter.CollectionMember;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.core.CoreQueryPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
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

    protected final static String QUERY = "SELECT * FROM Document WHERE collectionMember:collectionIds/* = ?";

    protected static final Long MAX_RESULT = (long) 50;

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
            final Repository repository = Framework.getLocalService(
                    RepositoryManager.class).getRepository(
                    doc.getRepositoryName());
            final CoreSession session;
            try {
                session = repository.open();
            } catch (Exception e) {
                throw new ClientException(e);
            }

            try {

                new UnrestrictedSessionRunner(session) {

                    @Override
                    public void run() throws ClientException {

                        List<DocumentModel> results = null;

                        // Bash style
                        TransactionHelper.commitOrRollbackTransaction();
                        TransactionHelper.startTransaction();

                        do {
                            boolean succeed = false;

                            try {
                                PageProviderService pps = Framework.getLocalService(PageProviderService.class);
                                Map<String, Serializable> props = new HashMap<String, Serializable>();
                                props.put(
                                        CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY,
                                        (Serializable) session);

                                CoreQueryPageProviderDescriptor desc = new CoreQueryPageProviderDescriptor();
                                desc.setPattern(QUERY);
                                desc.getProperties().put("maxResults",
                                        MAX_RESULT.toString());

                                Object[] parameters = new Object[1];
                                parameters[0] = doc.getId();

                                results = (DocumentModelList) pps.getPageProvider(
                                        "", desc, null, null, MAX_RESULT,
                                        (long) 0, props, parameters).getCurrentPage();

                                for (DocumentModel d : results) {
                                    log.trace(String.format(
                                            "Updating CollectionMember %s",
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
                                }
                            }
                        } while (results != null && !results.isEmpty());

                    }

                }.runUnrestricted();

            } finally {
                Repository.close(session);
            }
        }
    }

}
