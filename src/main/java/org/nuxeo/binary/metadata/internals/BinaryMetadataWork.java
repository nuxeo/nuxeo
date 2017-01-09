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
 *     vpasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.binary.metadata.internals;

import java.util.List;

import org.nuxeo.binary.metadata.api.BinaryMetadataService;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.runtime.api.Framework;

/**
 * Work handling binary metadata updates.
 *
 * @since 7.2
 */
public class BinaryMetadataWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    private static final String BINARY_METADATA_WORK = "binary_metadata_work";

    public static final String BINARY_METADATA_WORK_TITLE = "Binary Metadata Update Worker";

    protected final List<MetadataMappingDescriptor> mappingDescriptors;

    protected final String docId;

    public BinaryMetadataWork(String repositoryName, String docId, List<MetadataMappingDescriptor> mappingDescriptors) {
        super("BinaryMetadataUpdate|docId=" + docId);
        setDocument(repositoryName, docId);
        this.mappingDescriptors = mappingDescriptors;
        this.docId = docId;
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
    public void work() {
        setProgress(Progress.PROGRESS_INDETERMINATE);
        setStatus("Updating Metadata");
        openSystemSession();
        if (!session.exists(new IdRef(docId))) {
            setStatus("Nothing to process");
            return;
        }
        BinaryMetadataService binaryMetadataService = Framework.getLocalService(BinaryMetadataService.class);
        DocumentModel workingDocument = session.getDocument(new IdRef(docId));
        binaryMetadataService.handleUpdate(mappingDescriptors, workingDocument);
        setStatus("Metadata Update Done");
    }

}
