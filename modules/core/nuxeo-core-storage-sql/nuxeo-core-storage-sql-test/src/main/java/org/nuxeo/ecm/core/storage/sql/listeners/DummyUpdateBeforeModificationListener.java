/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat
 */
package org.nuxeo.ecm.core.storage.sql.listeners;

import java.util.Random;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

public class DummyUpdateBeforeModificationListener implements EventListener {

    protected static final Random RANDOM = new Random(); // NOSONAR (doesn't need cryptographic strength)

    public static final String PERDORM_UPDATE_FLAG = DummyUpdateBeforeModificationListener.class.getName()
            + "-force-update";

    @Override
    public void handleEvent(Event event) {
        if (DocumentEventTypes.BEFORE_DOC_UPDATE.equals(event.getName())) {
            DocumentEventContext context = (DocumentEventContext) event.getContext();
            DocumentModel doc = context.getSourceDocument();
            if (Boolean.TRUE.equals(doc.getContextData(PERDORM_UPDATE_FLAG))) {
                doc.setPropertyValue("dc:description", "auto" + RANDOM.nextDouble());
            }
        }
    }

}
