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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.listener.AbstractEventListener;
import org.nuxeo.ecm.core.listener.AsynchronousEventListener;
import org.nuxeo.ecm.wiki.WikiTypes;

public class WikiListener extends AbstractEventListener implements AsynchronousEventListener {

    private static final Log log = LogFactory.getLog(WikiListener.class);

    @Override
    public void handleEvent(CoreEvent coreEvent) throws Exception {
        /* TODO: work in progress
         * this is not working yet
         */

        Object source = coreEvent.getSource();
        if (source instanceof DocumentModel) {
            DocumentModel doc = (DocumentModel) source;
            final String type = doc.getType();
            String eventId = coreEvent.getEventId();
            if (WikiTypes.WIKIPAGE.equals(type) && DOCUMENT_UPDATED.equals(eventId)) {
                WikiHelper.updateRelations(doc);
            }
        }
    }

}
