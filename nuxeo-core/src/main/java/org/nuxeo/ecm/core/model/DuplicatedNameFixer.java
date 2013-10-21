/*******************************************************************************
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
/**
 *
 */

package org.nuxeo.ecm.core.model;

import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

/**
 * @author Stephane Lacoin at Nuxeo (aka matic)
 */
public class DuplicatedNameFixer implements EventListener {

    @Override
    public void handleEvent(Event event) throws ClientException {
        DocumentEventContext context = (DocumentEventContext) event.getContext();
        DocumentModel source = context.getSourceDocument();
        String name = source.getName();
        CoreSession session = context.getCoreSession();
        if (name == null || name.length() == 0) {
            name = IdUtils.generateStringId();
        }
        DocumentRef parentRef;
        String eventName = event.getName();
        if (DocumentEventTypes.ABOUT_TO_CREATE.equals(eventName)) {
            parentRef = source.getParentRef();
        } else if (DocumentEventTypes.ABOUT_TO_MOVE.equals(eventName)) {
            parentRef = (DocumentRef) context.getProperties().get(
                    CoreEventConstants.DESTINATION_REF);
        } else if (DocumentEventTypes.ABOUT_TO_IMPORT.equals(eventName)) {
            parentRef = source.getParentRef();
        } else {
            throw new IllegalArgumentException("Unknown event " + eventName);
        }
        if (parentRef == null) {
            return;
        }
        DocumentModel parent = session.getDocument(parentRef);
        PathRef path = new PathRef(parent.getPathAsString(), name);
        if (session.exists(path)) {
            name += '.' + String.valueOf(System.currentTimeMillis());
            source.setPathInfo(
                    source.getPath().removeLastSegments(1).toString(), name);
        }
    }

}
