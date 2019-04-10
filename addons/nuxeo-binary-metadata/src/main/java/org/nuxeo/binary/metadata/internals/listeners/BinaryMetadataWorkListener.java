/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;

import java.util.LinkedList;

import org.nuxeo.binary.metadata.api.BinaryMetadataConstants;
import org.nuxeo.binary.metadata.internals.BinaryMetadataWork;
import org.nuxeo.binary.metadata.internals.MetadataMappingDescriptor;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Handle document and blob updates according to {@link BinaryMetadataSyncListener} rules. If
 * {@link org.nuxeo.binary.metadata.api.BinaryMetadataConstants#ASYNC_BINARY_METADATA_EXECUTE} flag is set into Document
 * Event Context, a work should be executed.
 *
 * @since 7.2
 */
public class BinaryMetadataWorkListener implements EventListener {

    @Override
    public void handleEvent(Event event) {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }
        if (DOCUMENT_CREATED.equals(event.getName()) || DOCUMENT_UPDATED.equals(event.getName())) {
            DocumentEventContext docCtx = (DocumentEventContext) ctx;
            DocumentModel doc = docCtx.getSourceDocument();
            Boolean execute = (Boolean) doc.getContextData(BinaryMetadataConstants.ASYNC_BINARY_METADATA_EXECUTE);
            doc.putContextData(BinaryMetadataConstants.ASYNC_BINARY_METADATA_EXECUTE, null);
            if (Boolean.TRUE.equals(execute) && !doc.isProxy()) {
                LinkedList<MetadataMappingDescriptor> mappingDescriptors = (LinkedList<MetadataMappingDescriptor>) doc.getContextData(BinaryMetadataConstants.ASYNC_MAPPING_RESULT);
                doc.putContextData(BinaryMetadataConstants.ASYNC_MAPPING_RESULT, null);
                BinaryMetadataWork work = new BinaryMetadataWork(doc.getRepositoryName(), doc.getId(),
                        mappingDescriptors);
                WorkManager workManager = Framework.getLocalService(WorkManager.class);
                workManager.schedule(work, true);
            }
        }
    }

}
