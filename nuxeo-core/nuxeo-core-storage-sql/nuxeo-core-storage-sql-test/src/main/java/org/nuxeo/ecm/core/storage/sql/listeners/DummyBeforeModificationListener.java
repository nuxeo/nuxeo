/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

public class DummyBeforeModificationListener implements EventListener {

    // checked by unit test
    public static String previousTitle = null;

    /**
     * Called on aboutToCreate and beforeDocumentModification events.
     */
    @Override
    public void handleEvent(Event event) {
        DocumentEventContext context = (DocumentEventContext) event.getContext();
        // record previous title
        DocumentModel previous = (DocumentModel) context.getProperty(CoreEventConstants.PREVIOUS_DOCUMENT_MODEL);
        if (previous != null) {
            // beforeDocumentModification
            previousTitle = previous.getTitle();
        }
        // do the event job: rename
        DocumentModel doc = context.getSourceDocument();
        String name = doc.getTitle() + "-rename";
        context.setProperty(CoreEventConstants.DESTINATION_NAME, name);
    }

}
