/*
 * Copyright (c) 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
    public void handleEvent(Event event) throws ClientException {
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
        String parentPath = doc.getPath().removeLastSegments(1).toString();
        doc.setPathInfo(parentPath, name);
    }

}
