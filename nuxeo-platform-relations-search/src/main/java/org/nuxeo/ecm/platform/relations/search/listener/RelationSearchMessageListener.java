/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: RelationSearchMessageListener.java 21825 2007-07-03 11:16:01Z janguenot $
 */

package org.nuxeo.ecm.platform.relations.search.listener;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.common.SearchServiceDelegate;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.event.RelationEvents;
import org.nuxeo.ecm.platform.relations.search.indexer.RelationIndexer;

/**
 * Search message listener.
 * <p>
 * Listen for messages on the NXP topic to trigger operations against the search
 * engine service.
 *
 * @author <a href="mailto:gr@nuxeo.com">Georges Racinet</a>
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "topic/NXPMessages"),
        @ActivationConfigProperty(propertyName = "providerAdapterJNDI", propertyValue = "java:/NXCoreEventsProvider"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
@TransactionManagement(TransactionManagementType.CONTAINER)
public class RelationSearchMessageListener implements MessageListener {

    private static final Log log = LogFactory.getLog(RelationSearchMessageListener.class);

    private transient RelationIndexer indexer;

    private transient SearchService service;

    private SearchService getSearchService() {
        if (service == null) {
            service = SearchServiceDelegate.getRemoteSearchService();
        }
        return service;
    }

    private void initIndexer() {
        if (indexer == null) {
            indexer = new RelationIndexer();
        }
    }


    @SuppressWarnings("unchecked")
    public void onMessage(Message message) {

        try {
            SearchService service = getSearchService();
            initIndexer();

            // Check if the search service is active
            if (!service.isEnabled()) {
                return;
            }

            Serializable obj = ((ObjectMessage) message).getObject();
            if (!(obj instanceof DocumentMessage )) {
                return;
            }
            DocumentMessage doc = (DocumentMessage) obj;
            if (!RelationEvents.CATEGORY.equals(doc.getCategory())) {
                return; // caller is trustworthy
            }
            String eventId = doc.getEventId();

            Map<String, Serializable> eventInfo = doc.getEventInfo();
            List<Statement> statements =
                (List<Statement>) eventInfo.get(
                        RelationEvents.STATEMENTS_EVENT_KEY);

            if (RelationEvents.AFTER_RELATION_CREATION.equals(eventId)
                    || RelationEvents.AFTER_RELATION_MODIFICATION.equals(eventId)) {
                if (eventInfo == null) { // not likely
                    indexer.index(doc); // brutal indexing
                    return;
                }

                String graphName = (String) eventInfo.get(
                        RelationEvents.GRAPH_NAME_EVENT_KEY);
                if (statements == null) {
                    log.warn("Got a relation event, without associated " +
                            "graph name or statements, " +
                            "have to reindex all statements for document");
                    indexer.index(doc);
                }
                indexer.indexStatements(graphName, statements,
                        doc.getSessionId());
            }

            if (RelationEvents.AFTER_RELATION_REMOVAL.equals(eventId)) {
                indexer.unIndexStatements(statements);
                return;
            }

            log.warn("Got an unknown relation event, id=" + eventId);

        } catch (Exception e) {
            throw new EJBException(e);
        }
    }

}
