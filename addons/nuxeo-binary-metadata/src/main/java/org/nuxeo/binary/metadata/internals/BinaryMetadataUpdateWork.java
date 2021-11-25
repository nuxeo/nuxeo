/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.binary.metadata.internals;

import static java.lang.Boolean.TRUE;
import static org.nuxeo.binary.metadata.api.BinaryMetadataConstants.DISABLE_BINARY_METADATA_LISTENER;
import static org.nuxeo.ecm.core.api.versioning.VersioningService.DISABLE_AUTOMATIC_VERSIONING;
import static org.nuxeo.ecm.core.api.versioning.VersioningService.DISABLE_AUTO_CHECKOUT;
import static org.nuxeo.ecm.core.bulk.action.SetPropertiesAction.PARAM_DISABLE_AUDIT;

import java.util.List;

import org.nuxeo.binary.metadata.api.BinaryMetadataService;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 2021.13
 */
public class BinaryMetadataUpdateWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    protected static final String BINARY_METADATA_WORK = "binary_metadata_work";

    protected static final String BINARY_METADATA_WORK_TITLE = "Binary Metadata Update Worker";

    protected final List<MetadataMappingUpdate> metadataUpdates;

    public BinaryMetadataUpdateWork(String repositoryName, String docId, List<MetadataMappingUpdate> metadataUpdates) {
        super("BinaryMetadataUpdate|docId=" + docId);
        setDocument(repositoryName, docId);
        this.metadataUpdates = metadataUpdates;
    }

    @Override
    public String getCategory() {
        return BINARY_METADATA_WORK;
    }

    @Override
    public String getTitle() {
        return BINARY_METADATA_WORK_TITLE;
    }

    @Override
    public boolean isIdempotent() {
        return false;
    }

    @Override
    public void work() {
        setProgress(Progress.PROGRESS_INDETERMINATE);
        setStatus("Updating Metadata");
        openSystemSession();
        if (!session.exists(new IdRef(docId))) {
            setStatus("Nothing to process");
            return;
        }
        BinaryMetadataService binaryMetadataService = Framework.getService(BinaryMetadataService.class);
        DocumentModel workingDocument = session.getDocument(new IdRef(docId));
        binaryMetadataService.applyUpdates(workingDocument, metadataUpdates);

        workingDocument.putContextData(DISABLE_BINARY_METADATA_LISTENER, TRUE);
        workingDocument.putContextData(DISABLE_AUTO_CHECKOUT, TRUE);
        workingDocument.putContextData(DISABLE_AUTOMATIC_VERSIONING, TRUE);
        workingDocument.putContextData(PARAM_DISABLE_AUDIT, TRUE);
        workingDocument.putContextData("disableDublinCoreListener", TRUE);
        workingDocument.putContextData("disableNotificationService", TRUE);
        workingDocument.putContextData("disablePictureViewsGenerationListener", TRUE);
        workingDocument.putContextData("disableThumbnailComputation", TRUE);
        workingDocument.putContextData("disableVideoConversionsGenerationListener", TRUE);
        session.saveDocument(workingDocument);
        setStatus("Metadata Update Done");
    }
}
