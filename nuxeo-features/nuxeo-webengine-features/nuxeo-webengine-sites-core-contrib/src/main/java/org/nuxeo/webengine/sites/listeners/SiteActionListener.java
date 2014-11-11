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
package org.nuxeo.webengine.sites.listeners;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_CREATE;
import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBCONTAINER_ISWEBCONTAINER;
import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBCONTAINER_NAME;
import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBCONTAINER_SCHEMA;
import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBCONTAINER_URL;
import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBSITE;
import static org.nuxeo.webengine.sites.utils.SiteConstants.WORKSPACE;

import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

/**
 * Site related actions listener. It performs when a mini-site is created.
 *
 * @author rux
 */
public class SiteActionListener implements EventListener {

    /**
     * Sets the url field and the site name (if not already set) to the name,
     * respectively the title of the document model.
     */
    public void handleEvent(Event event) throws ClientException {
        String eventId = event.getName();

        DocumentEventContext docCtx;
        if (event.getContext() instanceof DocumentEventContext) {
            docCtx = (DocumentEventContext) event.getContext();
        } else {
            return;
        }
        DocumentModel doc = docCtx.getSourceDocument();
        String documentType = doc.getType();
        if (!(WORKSPACE.equals(documentType) || WEBSITE.equals(documentType))) {
            return;
        }

        // avoid error if document does not hold the webcontainer schema
        if (!doc.hasSchema(WEBCONTAINER_SCHEMA)) {
            return;
        }

        if (ABOUT_TO_CREATE.equals(eventId)) {
            String url = doc.getName();
            url = URIUtils.quoteURIPathComponent(url, false);
            String documentWithSameURLQuery = "SELECT * FROM DOCUMENT where "
                    + WEBCONTAINER_URL + " STARTSWITH \"" + url + "\"";
            DocumentModelList documentWithSameURL = docCtx.getCoreSession().query(
                    documentWithSameURLQuery);
            if (!documentWithSameURL.isEmpty()) {
                // FIXME: this is not right
                int sameName = 0;
                url = url + "_" + (sameName + 1);
            }

            doc.setPropertyValue(WEBCONTAINER_URL, url);

            if (WEBSITE.equals(documentType)) {
                // Is WebSite
                // CB: Because, at least for a while, Workspaces need to work
                // together with WebSites, "isWebContainer" flag needs to be
                // kept and set to "true" for all new created WebSites.
                doc.setPropertyValue(WEBCONTAINER_ISWEBCONTAINER, Boolean.TRUE);
            }
        }

        // Set WebSite title
        doc.setPropertyValue(WEBCONTAINER_NAME, doc.getTitle());
    }
}
