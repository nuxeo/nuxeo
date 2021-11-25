/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */

package org.nuxeo.binary.metadata.internals.listeners;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;

import java.util.List;

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
 * @deprecated since 2021.13, {@link BinaryMetadataSyncListener} now handles the async updates
 */
@Deprecated
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
                List<MetadataMappingDescriptor> mappingDescriptors = (List<MetadataMappingDescriptor>) doc.getContextData(BinaryMetadataConstants.ASYNC_MAPPING_RESULT);
                doc.putContextData(BinaryMetadataConstants.ASYNC_MAPPING_RESULT, null);
                BinaryMetadataWork work = new BinaryMetadataWork(doc.getRepositoryName(), doc.getId(),
                        mappingDescriptors);
                WorkManager workManager = Framework.getService(WorkManager.class);
                workManager.schedule(work, true);
            }
        }
    }

}
