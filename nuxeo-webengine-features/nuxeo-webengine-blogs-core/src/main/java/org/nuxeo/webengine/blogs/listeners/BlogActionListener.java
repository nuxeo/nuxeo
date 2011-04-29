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
 *
 */
package org.nuxeo.webengine.blogs.listeners;

import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.webengine.blogs.utils.BlogConstants;
import org.nuxeo.webengine.sites.utils.SiteConstants;

/**
 * Blog related actions listener.
 *
 * @author <a href="mailto:cbaican@nuxeo.com">Catalin Baican</a>
 */
public class BlogActionListener implements EventListener {

    /**
     * Sets the url field and the blog name (if not already set) to the name,
     * respectively the title of the document model. Also the default theme
     * settings are set depending of the received document type.
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

        if (BlogConstants.BLOG_DOC_TYPE.equals(documentType)) {

            if (DocumentEventTypes.ABOUT_TO_CREATE.equals(eventId)) {

                // Is WebSite
                // CB: Because, at least for a while, Workspaces need to work
                // together with WebSites, "isWebContainer" flag needs to be
                // kept and set to "true" for all new created WebSites.
                // TODO probably the methods from the site modules that keep
                // this field into account, should be updated in order to ignore
                // it
                doc.setPropertyValue(SiteConstants.WEBCONTAINER_ISWEBCONTAINER,
                        Boolean.TRUE);
                // Set Blog url field
                String url = doc.getName();
                url = URIUtils.quoteURIPathComponent(url, false);
                doc.setPropertyValue(SiteConstants.WEBCONTAINER_URL, url);
                doc.setPropertyValue(SiteConstants.WEBSITE_SCHEMA_THEME,
                        "blogs");
                doc.setPropertyValue(SiteConstants.WEBSITE_THEMEPAGE, "site");
            }
            // Set Blog title
            doc.setPropertyValue(SiteConstants.WEBCONTAINER_NAME,
                    doc.getTitle());

        } else if (BlogConstants.BLOG_POST_DOC_TYPE.equals(documentType)) {
            doc.setPropertyValue(SiteConstants.WEBPAGE_SCHEMA_THEME, "blogs");
            doc.setPropertyValue(SiteConstants.WEBPAGE_THEMEPAGE, "post");
        }
    }

}
