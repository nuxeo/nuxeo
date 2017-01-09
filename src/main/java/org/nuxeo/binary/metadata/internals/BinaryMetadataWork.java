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
 *     vpasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.binary.metadata.internals;

import java.util.LinkedList;

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

    protected final LinkedList<MetadataMappingDescriptor> mappingDescriptors;

    protected final String docId;

    public BinaryMetadataWork(String repositoryName, String docId,
            LinkedList<MetadataMappingDescriptor> mappingDescriptors) {
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
        initSession();
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
