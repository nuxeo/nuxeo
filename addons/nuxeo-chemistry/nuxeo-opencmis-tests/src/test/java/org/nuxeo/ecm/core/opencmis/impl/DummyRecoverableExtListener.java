/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.RecoverableClientException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

/**
 * Throws a {@link RecoverableClientException} if the document name starts with "throw".
 */
public class DummyRecoverableExtListener implements EventListener {

    @Override
    public void handleEvent(Event event) {
        DocumentEventContext ctx = (DocumentEventContext) event.getContext();
        DocumentModel doc = ctx.getSourceDocument();
        if (doc.getName().startsWith("throw")) {
            event.markBubbleException();
            String[] params = new String[] { "400" };
            throw new RecoverableClientException("bad name", "bad name", params);
        }
    }

}
