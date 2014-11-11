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
 * $Id$
 */

package org.nuxeo.ecm.core.api;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.event.impl.CoreEventImpl;
import org.nuxeo.ecm.core.listener.CoreEventListenerService;

/**
 * Helper class to send versions change event notifications in the core.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public final class VersioningChangeNotifier {

    /**
     * Core event id for events dispatched by this class.
     */
    public static final String CORE_EVENT_ID_VERSIONING_CHANGE = "versioningChangeCoreEvent";

    /**
     * The key in the info map pointing to the frozen document (previous
     * version).
     */
    public static final String EVT_INFO_OLD_DOC_KEY = "oldDoc";

    /**
     * The key in the info map pointing to the current document.
     */
    public static final String EVT_INFO_NEW_DOC_KEY = "newDoc";

    private static final Log log = LogFactory.getLog(VersioningChangeNotifier.class);

    // Utility class.
    private VersioningChangeNotifier() {
    }

    /**
     * Sends change notifications to core event listeners. The event contains
     * info with older document (before version change) and newer doc (current
     * document).
     *
     * @param oldDocument
     * @param newDocument
     * @param options additional info to pass to the event
     */
    public static void notifyVersionChange(DocumentModel oldDocument,
            DocumentModel newDocument, Map<String, Object> options) {
        final Map<String, Object> info = new HashMap<String, Object>();
        if (options != null) {
            info.putAll(options);
        }
        info.put(EVT_INFO_NEW_DOC_KEY, newDocument);
        info.put(EVT_INFO_OLD_DOC_KEY, oldDocument);
        final CoreEvent coreEvent = new CoreEventImpl(
                CORE_EVENT_ID_VERSIONING_CHANGE, newDocument, info,
                AbstractSession.ANONYMOUS,
                DocumentEventCategories.EVENT_CLIENT_NOTIF_CATEGORY, null);
        notifyEvent(coreEvent);
    }

    private static void notifyEvent(final CoreEvent coreEvent) {
        CoreEventListenerService service = NXCore.getCoreEventListenerService();
        if (service != null) {
            log.debug("Notify RepositoryEventListener listeners list for event="
                    + coreEvent.getEventId());
            service.notifyEventListeners(coreEvent);
        } else {
            log.error("Impossible to notify core events ! "
                    + "CoreEventListenerService service is missing...");
        }
    }

}
