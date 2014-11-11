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

package org.nuxeo.ecm.wiki.listener;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.wiki.WikiTypes;

public class WikiListener  implements EventListener {

    protected DocumentModel doExtractWikiPage(Event event) {

         if (!DOCUMENT_UPDATED.equals(event.getName())) {
             return null;
         }

         final Object context = event.getContext();
         if (!(context instanceof DocumentEventContext)) {
             return null;
         }

         final DocumentModel doc = ((DocumentEventContext)context).getSourceDocument();
         if (!WikiTypes.WIKIPAGE.equals(doc.getType())) {
             return null;
         }

         return doc;
    }

    public void handleEvent(Event event) {
        DocumentModel wikiPage = doExtractWikiPage(event);
        if (wikiPage == null) {
            return;
        }
        WikiHelper.updateRelations(wikiPage);
    }

}
