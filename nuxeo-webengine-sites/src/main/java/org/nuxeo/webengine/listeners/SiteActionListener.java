/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.webengine.listeners;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.webengine.utils.SiteConstants;

/**
 * Site related actions listener. It performs when a mini-site is created.
 * @author rux
 */
public class SiteActionListener implements EventListener {

    /**
     * Sets the url field and the site name (if not already set) to the name, 
     * respectively the title of the document model. 
     */
    public void handleEvent(Event event) throws ClientException {
        String eventId = event.getName();

        if (!eventId.equals(DocumentEventTypes.DOCUMENT_CREATED)) {
            return;
        }

        DocumentEventContext docCtx;
        if (event.getContext() instanceof DocumentEventContext) {
            docCtx = (DocumentEventContext) event.getContext();
        } else {
            return;
        }
        DocumentModel doc = docCtx.getSourceDocument();
        if (!SiteConstants.WORKSPACE.equals(doc.getType())) {
            return;
        }

        doc.setPropertyValue(SiteConstants.WEBCONTAINER_URL, doc.getName());
        if (StringUtils.isEmpty((String) doc.getPropertyValue(
                SiteConstants.WEBCONATINER_NAME))) {
            doc.setPropertyValue(SiteConstants.WEBCONATINER_NAME, 
                    doc.getTitle());
        }
    }

}
