/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.model;

import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

/**
 * @author Stephane Lacoin at Nuxeo (aka matic)
 */
public class DuplicatedNameFixer implements EventListener {

    @Override
    public void handleEvent(Event event) {
        DocumentEventContext context = (DocumentEventContext) event.getContext();
        Boolean destinationExists = (Boolean) context.getProperty(CoreEventConstants.DESTINATION_EXISTS);
        if (!destinationExists) {
            return;
        }
        String name = (String) context.getProperty(CoreEventConstants.DESTINATION_NAME);
        name += '.' + String.valueOf(System.currentTimeMillis());
        context.setProperty(CoreEventConstants.DESTINATION_NAME, name);
    }

}
