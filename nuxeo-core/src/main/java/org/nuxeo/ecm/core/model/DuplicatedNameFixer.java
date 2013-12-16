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

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
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
        Boolean destinationExists = (Boolean)context.getProperty(CoreEventConstants.DESTINATION_EXISTS);
        if (!destinationExists) {
            return;
        }
        String name = (String)context.getProperty(CoreEventConstants.DESTINATION_NAME);
        name += '.' + String.valueOf(System.currentTimeMillis());
        context.setProperty(CoreEventConstants.DESTINATION_NAME, name);
    }

}
