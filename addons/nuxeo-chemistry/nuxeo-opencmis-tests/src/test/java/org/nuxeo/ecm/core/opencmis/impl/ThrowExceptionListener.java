/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.opencmis.impl;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.RecoverableClientException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

/**
 * Throws an exception after creating a doc to test rollback behavior.
 */
public class ThrowExceptionListener implements EventListener {

    public static final String CRASHME_NAME = "CRASHME";

    public static final String AFTERCRASH_NAME = "AFTERCRASH";

    @Override
    public void handleEvent(Event event) {
        DocumentEventContext ctx = (DocumentEventContext) event.getContext();
        DocumentModel doc = ctx.getSourceDocument();
        if (CRASHME_NAME.equals(doc.getName())) {
            // create another doc so that we can check it'll be rolled back correctly
            CoreSession session = doc.getCoreSession();
            DocumentModel doc2 = session.createDocumentModel("/", AFTERCRASH_NAME, "File");
            doc2 = session.createDocument(doc2);
            session.save();
            // now throw an exception
            // make sure it's not swallowed by EventServiceImpl
            event.markBubbleException();
            // RecoverableClientException to avoid ERROR logging in EventServiceImpl
            throw new RecoverableClientException("error", "error", null);
        }
    }

}
