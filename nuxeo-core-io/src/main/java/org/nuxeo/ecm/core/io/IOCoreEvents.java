/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.io;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.event.impl.CoreEventImpl;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.listener.CoreEventListenerService;
import org.nuxeo.ecm.core.listener.impl.CoreEventListenerServiceImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * A helper class to declare and send events related to I/O processing.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class IOCoreEvents {

    private static final Log log = LogFactory.getLog(IOCoreEvents.class);

    public static final String DOCUMENT_EXPORTED = "documentExported";

    public static final String DOCUMENT_IMPORTED = "documentImported";

    // Utility class.
    private IOCoreEvents() {
    }

    /**
     * Sends core events for the eventId for the given docs.
     *
     * @param docs
     * @param repositoryName
     * @param eventId
     * @throws ClientException
     */
    public static void notifyEvents(Collection<DocumentRef> docs,
            String repositoryName, String eventId) throws ClientException {

        Map<String, Object> options = new HashMap<String, Object>();
        options.put(CoreEventConstants.REPOSITORY_NAME, repositoryName);

        CoreSession coreSession = getCoreSession(repositoryName);
        for (DocumentRef docRef : docs) {
            DocumentModel docModel = coreSession.getDocument(docRef);
            notifyEvent(docModel, options, eventId);
        }
    }

    private static void notifyEvent(DocumentModel source,
            Map<String, Object> options, String eventId) {

        String category = DocumentEventCategories.EVENT_DOCUMENT_CATEGORY;

        CoreEvent coreEvent = new CoreEventImpl(eventId, source, options,
                AbstractSession.ANONYMOUS, category, null);

        CoreEventListenerService service = getCoreEventListenerService();

        if (service != null) {
            log.debug("Notify RepositoryEventListener listeners list for event="
                    + eventId);
            service.notifyEventListeners(coreEvent);
        } else {
            log.error("Impossible to notify core events ! "
                    + "CoreEventListenerService service is missing...");
        }
    }

    public static CoreEventListenerService getCoreEventListenerService() {
        return (CoreEventListenerService) Framework.getRuntime().getComponent(
                CoreEventListenerServiceImpl.NAME);
    }

    private static CoreSession getCoreSession(String repo)
            throws ClientException {
        CoreSession systemSession;
        try {
            Framework.login();
            RepositoryManager manager = Framework.getService(RepositoryManager.class);
            systemSession = manager.getRepository(repo).open();
        } catch (Exception e) {
            throw new ClientException(
                    "Failed to open core session to repository " + repo, e);
        }
        return systemSession;
    }

}
