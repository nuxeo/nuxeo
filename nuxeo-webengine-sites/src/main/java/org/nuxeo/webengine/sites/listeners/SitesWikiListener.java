/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.webengine.sites.listeners;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;
import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBPAGE;
import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBPAGE_EDITOR;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.webengine.sites.utils.SitesRelationsWikiHelper;

/**
 * @author <a href="mailto:cbaican@nuxeo.com">Catalin Baican</a>
 *
 */
public class SitesWikiListener  implements EventListener {

    private static final Log log = LogFactory.getLog(SitesWikiListener.class);

    protected DocumentModel doExtractWebPage(Event event) {

         if (!(DOCUMENT_UPDATED.equals(event.getName()) || DOCUMENT_CREATED.equals(event.getName()))) {
             return null;
         }

         final Object context = event.getContext();
         if (!(context instanceof DocumentEventContext)) {
             return null;
         }

         final DocumentModel doc = ((DocumentEventContext)context).getSourceDocument();
         if (!WEBPAGE.equals(doc.getType())) {
             return null;
         }

         return doc;
    }

    public void handleEvent(Event event) {
        DocumentModel webPage = doExtractWebPage(event);

        if (webPage == null) {
            return;
        }
        Boolean isRichText = Boolean.TRUE;
        try {
            isRichText = (Boolean) webPage.getPropertyValue(WEBPAGE_EDITOR);
        } catch (ClientException e) {
            log.error("SitesWikiListener error...", e);
        }
        if (isRichText) {
            return;
        }

        SitesRelationsWikiHelper.updateRelations(webPage);
    }

}
