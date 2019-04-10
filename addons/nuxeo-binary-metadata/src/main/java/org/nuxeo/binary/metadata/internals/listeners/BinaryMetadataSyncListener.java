/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */

package org.nuxeo.binary.metadata.internals.listeners;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_CREATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;

import org.nuxeo.binary.metadata.api.BinaryMetadataConstants;
import org.nuxeo.binary.metadata.api.BinaryMetadataService;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * Handle document and blob updates according to following rules in an event context: - Define if rule should be
 * executed in async or sync mode. - If Blob dirty and document metadata dirty, write metadata from doc to Blob. - If
 * Blob dirty and document metadata not dirty, write metadata from Blob to doc. - If Blob not dirty and document
 * metadata dirty, write metadata from doc to Blob.
 *
 * @since 7.1
 */
public class BinaryMetadataSyncListener implements EventListener {

    @Override
    public void handleEvent(Event event) {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }
        BinaryMetadataService binaryMetadataService = Framework.getLocalService(BinaryMetadataService.class);
        DocumentEventContext docCtx = (DocumentEventContext) ctx;
        DocumentModel doc = docCtx.getSourceDocument();
        Boolean disable = (Boolean) event.getContext().getProperty(
                BinaryMetadataConstants.DISABLE_BINARY_METADATA_LISTENER);
        if (ABOUT_TO_CREATE.equals(event.getName()) && !doc.isProxy() && disable == null) {
            binaryMetadataService.writeMetadata(doc);
        } else if (BEFORE_DOC_UPDATE.equals(event.getName()) && !doc.isProxy() && disable == null) {
            doc.putContextData(BinaryMetadataConstants.DISABLE_BINARY_METADATA_LISTENER, Boolean.TRUE);
            binaryMetadataService.handleSyncUpdate(doc);
        }
    }

}
