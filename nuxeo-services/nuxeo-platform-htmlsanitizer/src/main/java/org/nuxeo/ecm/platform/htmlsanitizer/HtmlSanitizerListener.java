/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.htmlsanitizer;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener that sanitizes some HTML fields to remove potential cross-site scripting attacks in them.
 */
public class HtmlSanitizerListener implements EventListener {

    public static final String DISABLE_HTMLSANITIZER_LISTENER = "disableHtmlSanitizerListener";

    @Override
    public void handleEvent(Event event) {
        String eventId = event.getName();
        if (!eventId.equals(DocumentEventTypes.ABOUT_TO_CREATE)
                && !eventId.equals(DocumentEventTypes.BEFORE_DOC_UPDATE)) {
            return;
        }
        EventContext context = event.getContext();
        if (!(context instanceof DocumentEventContext)) {
            return;
        }
        Boolean disableListener = (Boolean) context.getProperty(DISABLE_HTMLSANITIZER_LISTENER);
        if (Boolean.TRUE.equals(disableListener)) {
            return;
        }

        DocumentModel doc = ((DocumentEventContext) context).getSourceDocument();
        if (doc.hasFacet(FacetNames.IMMUTABLE)) {
            return;
        }
        HtmlSanitizerService sanitizer = Framework.getService(HtmlSanitizerService.class);
        sanitizer.sanitizeDocument(doc);
    }

}
