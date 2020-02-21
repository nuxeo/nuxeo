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
 *     Nuxeo
 */

package org.nuxeo.apidoc.listener;

import static org.nuxeo.apidoc.listener.AttributesExtractorScheduler.EXTRACT_XML_ATTRIBUTES_NEEDED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_CREATE;

import java.util.Arrays;
import java.util.List;

import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

/**
 * Listener triggered on "aboutTo*" events to let the other lister
 * org.nuxeo.apidoc.listener.AttributeExtractorWorkerListener to trigger Worker when blob are ready to be extracted.
 *
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 8.3
 */
public class AttributesExtractorStater implements EventListener {

    public static final String ATTRIBUTES_PROPERTY = "adc:attributes";

    public static final List<String> DOC_TYPES = Arrays.asList(ExtensionPointInfo.TYPE_NAME, ExtensionInfo.TYPE_NAME);

    @Override
    public void handleEvent(Event event) {
        if (!(event.getContext() instanceof DocumentEventContext)) {
            return;
        }

        DocumentEventContext ctx = (DocumentEventContext) event.getContext();
        DocumentModel doc = ctx.getSourceDocument();
        if (!DOC_TYPES.contains(doc.getType())) {
            return;
        }

        Property fileProperty = doc.getProperty(NuxeoArtifact.CONTENT_PROPERTY_PATH);
        // Handling "migration case", when a blob is present but without any
        // attributes.
        boolean force = fileProperty.getValue() != null && doc.getPropertyValue(ATTRIBUTES_PROPERTY) == null;
        if (!(force || fileProperty.isDirty() || ABOUT_TO_CREATE.equals(event.getName()))) {
            return;
        }

        Blob blob = (Blob) fileProperty.getValue();
        if (blob == null) {
            doc.setPropertyValue(ATTRIBUTES_PROPERTY, null);
        } else {
            // Property will be read by
            // org.nuxeo.apidoc.listener.AttributeExtractorWorkerListener to
            // trigger worker when everything is good.
            // Worker cannot be triggered on "aboutTo*" events, and dirty props
            // cannot be checked post "aboutTo*" events
            ctx.setProperty(EXTRACT_XML_ATTRIBUTES_NEEDED, true);
        }
    }
}
