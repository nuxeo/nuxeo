/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.listeners;

import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

public class DummyAsyncRetryListener implements PostCommitEventListener {

    protected static int countHandled;

    protected static int countOk;

    @Override
    public void handleEvent(EventBundle events) {
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
        if (doc != null && ((String) doc.getPropertyValue("dc:title")).startsWith("title")) {
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
