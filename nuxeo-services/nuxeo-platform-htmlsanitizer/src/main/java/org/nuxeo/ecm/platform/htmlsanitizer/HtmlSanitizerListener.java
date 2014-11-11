/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.htmlsanitizer;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener that sanitizes some HTML fields to remove potential cross-site
 * scripting attacks in them.
 */
public class HtmlSanitizerListener implements EventListener {

    public void handleEvent(Event event) throws ClientException {
        String eventId = event.getName();
        if (!eventId.equals(DocumentEventTypes.ABOUT_TO_CREATE)
                && !eventId.equals(DocumentEventTypes.BEFORE_DOC_UPDATE)) {
            return;
        }
        EventContext context = event.getContext();
        if (!(context instanceof DocumentEventContext)) {
            return;
        }
        DocumentModel doc = ((DocumentEventContext) context).getSourceDocument();
        if (doc.hasFacet(FacetNames.IMMUTABLE)) {
            return;
        }
        HtmlSanitizerService sanitizer;
        try {
            sanitizer = Framework.getService(HtmlSanitizerService.class);
        } catch (Exception e) {
            throw new ClientException("Cannot sanitize", e);
        }
        sanitizer.sanitizeDocument(doc);
    }

}
