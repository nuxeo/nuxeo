/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.collections.core.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * Event handler to restored the collection members of a restored version of a collection.
 *
 * @since 7.3
 */
public class RestoredCollectionListener implements EventListener {

    private static final Log log = LogFactory.getLog(RestoredCollectionListener.class);

    @Override
    public void handleEvent(Event event) throws ClientException {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }

        final String eventId = event.getName();

        final DocumentEventContext docCxt = (DocumentEventContext) event.getContext();
        final CollectionManager collectionManager = Framework.getLocalService(CollectionManager.class);

        DocumentModel doc = null;
        DocumentModel version = null;
        if (eventId.equals(DocumentEventTypes.BEFORE_DOC_RESTORE)) {
            doc = docCxt.getSourceDocument();
            final String versionRefId = (String) docCxt.getProperties().get(
                    VersioningDocument.RESTORED_VERSION_UUID_KEY);
            version = docCxt.getCoreSession().getDocument(new IdRef(versionRefId));
            if (!collectionManager.isCollection(doc)) {
                return;
            }
        } else {
            return;
        }

        log.trace(String.format("Collection %s restored", doc.getId()));

        collectionManager.processRestoredCollection(doc, version);

    }

}
