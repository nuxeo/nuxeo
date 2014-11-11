/*
 * Copyright (c) 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.listeners;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

public class DummyAsyncRetryListener implements PostCommitEventListener {

    protected static int countHandled;

    protected static int countOk;

    @Override
    public void handleEvent(EventBundle events) throws ClientException {
        countHandled++;

        // accessing the iterator reconnects the events
        DocumentModel doc = null;
        for (Event event : events) {
            EventContext context = event.getContext();
            if (!(context instanceof DocumentEventContext)) {
                continue;
            }
            DocumentEventContext documentEventContext = (DocumentEventContext) context;
            doc = documentEventContext.getSourceDocument();
        }

        if (countHandled == 1) {
            // simulate error
            throw new ConcurrentUpdateException();
        }
        if (doc != null
                && ((String) doc.getPropertyValue("dc:title")).startsWith("title")) {
            countOk++;
        }
    }

    public static void clear() {
        countHandled = 0;
        countOk = 0;
    }

    public static int getCountHandled() {
        return countHandled;
    }

    public static int getCountOk() {
        return countOk;
    }

}
