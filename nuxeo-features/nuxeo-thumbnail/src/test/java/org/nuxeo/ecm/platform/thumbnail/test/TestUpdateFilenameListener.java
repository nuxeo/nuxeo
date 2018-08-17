/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.platform.thumbnail.test;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.thumbnail.listener.CheckBlobUpdateListener;

/**
 * Dummy listener that sets the filename of the event document's content to a constant string.
 * <p>
 * The purpose is to make the content property dirty when {@link CheckBlobUpdateListener} is called. Note: such a
 * complex property is marked as dirty though its internal properties might not have changed.
 *
 * @since 10.3
 */
public class TestUpdateFilenameListener implements EventListener {

    protected static final String FILENAME = "test.jpg";

    @Override
    public void handleEvent(Event event) {
        EventContext eventContext = event.getContext();
        if (!(eventContext instanceof DocumentEventContext)) {
            return;
        }
        DocumentEventContext documentEventContext = (DocumentEventContext) eventContext;
        DocumentModel doc = documentEventContext.getSourceDocument();
        Property property = doc.getProperty("file:content");
        Blob blob = (Blob) property.getValue();
        blob.setFilename(FILENAME);
        property.setValue(blob);
    }

}
