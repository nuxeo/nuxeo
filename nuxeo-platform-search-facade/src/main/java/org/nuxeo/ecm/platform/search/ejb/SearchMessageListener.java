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
 * $Id: SearchMessageListener.java 30393 2008-02-21 07:03:54Z sfermigier $
 */

package org.nuxeo.ecm.platform.search.ejb;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.security.auth.login.LoginContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.search.NXSearch;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.events.IndexingEventConf;
import org.nuxeo.ecm.core.search.api.indexingwrapper.DocumentModelIndexingWrapper;
import org.nuxeo.ecm.core.search.threading.IndexingThreadPool;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.events.api.EventMessage;
import org.nuxeo.ecm.platform.events.api.JMSConstant;
import org.nuxeo.runtime.api.Framework;

/**
 * Search message listener.
 *
 * <p>
 * Listen for messages on the NXP topic to trigger operations against the search
 * engine service.
 * </p>
 *
 * <p>
 * Note, this mdb should be deployed on the same node as the core search service
 * for now.
 * </p>
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "topic/NXPMessages"),
        @ActivationConfigProperty(propertyName = "providerAdapterJNDI", propertyValue = "java:/NXCoreEventsProvider"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = JMSConstant.NUXEO_MESSAGE_TYPE + " IN ('"
                + JMSConstant.DOCUMENT_MESSAGE + "','" + JMSConstant.EVENT_MESSAGE + "')") })
@TransactionManagement(TransactionManagementType.CONTAINER)
public class SearchMessageListener implements MessageListener {

    private static final Log log = LogFactory.getLog(SearchMessageListener.class);

    private transient SearchService service;

    private LoginContext loginCtx;

    private static final String ASYNC_INDEXING_DELAY_PROPERTY = "org.nuxeo.ecm.platform.search.asynchronousIndexingDelay";

    private SearchService getSearchService() {
        if (service == null) {
            // XXX : use local interface since the MDB is part of the Facade
            // package
            // service = SearchServiceDelegate.getRemoteSearchService();
            service = NXSearch.getSearchService();
        }
        return service;
    }

    private void login() throws Exception {
        loginCtx = Framework.login();
    }

    private void logout() throws Exception {
        if (loginCtx != null) {
            loginCtx.logout();
        }
    }

    public void onMessage(Message message) {

        try {
            login();
        } catch (Exception e) {
            throw new EJBException(e);
        }

        // :XXX: deal with other events such as audit, relations, etc...

        try {

            SearchService service = getSearchService();

            // Check if the search service is active
            if (!service.isEnabled()) {
                return;
            }

            Object obj = ((ObjectMessage)message).getObject();
            if(!(obj instanceof DocumentMessage))
                return;
            DocumentMessage doc = (DocumentMessage) obj;
            String eventId = doc.getEventId();

            Boolean duplicatedMessage = (Boolean) doc.getEventInfo().get(
                    EventMessage.DUPLICATED);
            if (duplicatedMessage != null && duplicatedMessage == true) {
                return;
            }

            IndexingEventConf eventConf = service.getIndexingEventConfByName(eventId);
            if (eventConf == null) {
                log.debug("not interested about event with id=" + eventId);
                return;
            }

            if (eventConf.getMode().equals(IndexingEventConf.ONLY_SYNC)) {
                log.debug("Event with id=" + eventId
                        + " should only be processed in sync");
                return;
            }

            if (eventConf.getMode().equals(IndexingEventConf.NEVER)) {
                log.debug("Event with id=" + eventId
                        + " is desactivated for indexing");
                return;
            }

            boolean recursive = eventConf.isRecursive();
            String action = eventConf.getAction();

            // get the wrapper if available
            DocumentModel dm = doc.getAdapter(DocumentModelIndexingWrapper.class);
            if (IndexingEventConf.INDEX.equals(action)
                    || IndexingEventConf.RE_INDEX.equals(action)) {

                // Check if the dm is indexable.
                // For now only based on explicit registration of the doc type
                // against the search service. (i.e : versus core type facet
                // based)
                if (service.getIndexableDocTypeFor(dm.getType()) == null) {
                    return;
                }

                if (log.isDebugEnabled()) {
                    log.debug("indexing " + dm.getPath());
                }

                String asynchronousIndexingDelay = Framework.getProperty(ASYNC_INDEXING_DELAY_PROPERTY);
                if (asynchronousIndexingDelay != null && asynchronousIndexingDelay.length() > 0) {
                    long asyncIndexingDelayValue = 0;
                    try {
                        asyncIndexingDelayValue = Long.parseLong(asynchronousIndexingDelay);
                    } catch (NumberFormatException e) {
                        log.error("Wrong asynchronous indexing delay specified", e);
                    }
                    if (asyncIndexingDelayValue > 0) {
                        try {
                            Thread.sleep(asyncIndexingDelayValue);
                        } catch (InterruptedException e) {
                            log.error("Couldn't wait before asynchronous indexing", e);
                        }
                    }
                }

                // Compute full text as well.
                IndexingThreadPool.index(dm, recursive, true);
            } else if (IndexingEventConf.UN_INDEX.equals(action)) {
                if (log.isDebugEnabled()) {
                    log.debug("asynchronous unindexing " + dm.getPath());
                }
                IndexingThreadPool.unindex(dm, recursive);// Compute
            }
        } catch (Exception e) {
            throw new EJBException(e);
        } finally {
            try {
                logout();
            } catch (Exception e) {
                log.error("Impossible to logout", e);
            }
        }
    }

}
