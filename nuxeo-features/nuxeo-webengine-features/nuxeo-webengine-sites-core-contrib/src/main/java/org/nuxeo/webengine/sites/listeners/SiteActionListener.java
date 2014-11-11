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

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.webengine.sites.utils.SiteConstants;

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

        if (!(DocumentEventTypes.ABOUT_TO_CREATE.equals(eventId) || DocumentEventTypes.BEFORE_DOC_UPDATE.equals(eventId))) {
            return;
        }

        DocumentEventContext docCtx;
        if (event.getContext() instanceof DocumentEventContext) {
            docCtx = (DocumentEventContext) event.getContext();
        } else {
            return;
        }
        DocumentModel doc = docCtx.getSourceDocument();
        String documentType = doc.getType();
        if (!(SiteConstants.WORKSPACE.equals(documentType) || SiteConstants.WEBSITE.equals(documentType))) {
            return;
        }

        if (DocumentEventTypes.ABOUT_TO_CREATE.equals(eventId)) {

            String url = doc.getName();
            int sameName = 0;
            DocumentModelList otherChildren = docCtx.getCoreSession().getChildren(doc.getParentRef());
            for (DocumentModel child : otherChildren) {
                if (child.getType().equals(doc.getType())) {
                    if (child.getName().startsWith(url)) {
                        sameName+=1;
                    }
                }
            }
            if (sameName>0) {
                url = url + "_" + (sameName+1);
            }

            doc.setPropertyValue(SiteConstants.WEBCONTAINER_URL, url);

            if (SiteConstants.WEBSITE.equals(documentType)) {
                // Is WebSite
                // CB: Because, at least for a while, Workspaces need to work
                // together with WebSites, "isWebContainer" flag needs to be
                // kept and set to "true" for all new created WebSites.
                doc.setPropertyValue(SiteConstants.WEBCONTAINER_ISWEBCONTAINER,
                        Boolean.TRUE);
            }
        }

        // Set WebSite title
        doc.setPropertyValue(SiteConstants.WEBCONTAINER_NAME, doc.getTitle());
    }

}
