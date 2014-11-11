/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.event.test;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

public class ShallowFilterPostCommitEventListener implements
        PostCommitFilteringEventListener {

    public static volatile int handledCount;

    @Override
    public void handleEvent(EventBundle events) throws ClientException {
        handledCount++;
    }

    @Override
    public boolean acceptEvent(Event event) {
        if (!(event.getContext() instanceof DocumentEventContext)) {
            return false;
        }
        DocumentEventContext ctx = (DocumentEventContext) event.getContext();
        try {
            DocumentModel doc = ctx.getSourceDocument();
            if (doc == null) {
                return false;
            }
            return doc.getCurrentLifeCycleState().equals("undefined");
        } catch (ClientException e) {
            throw new ClientRuntimeException(
                    "Cannot access to shallowed property");
        }
    }

}
