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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.operations;

import org.nuxeo.drive.adapter.impl.FileSystemItemHelper;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.VersioningFileSystemItemFactory;
import org.nuxeo.drive.service.impl.FileSystemItemAdapterServiceImpl;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.runtime.api.Framework;

/**
 * Updates the given {@link DocumentModel} with the given blob using the versioning policy of the given
 * {@link VersioningFileSystemItemFactory}.
 *
 * @author Antoine Taillefer
 * @since 7.4
 */
@Operation(id = NuxeoDriveAttachBlob.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Attach blob")
public class NuxeoDriveAttachBlob {

    public static final String ID = "NuxeoDrive.AttachBlob";

    @Context
    protected CoreSession session;

    @Param(name = "document")
    protected DocumentModel doc;

    @Param(name = "applyVersioningPolicy", required = false, values = "false")
    protected boolean applyVersioningPolicy = false;

    @Param(name = "factoryName", required = false, values = "defaultFileSystemItemFactory")
    protected String factoryName = "defaultFileSystemItemFactory";

    @OperationMethod
    public Blob run(Blob blob) {
        if (applyVersioningPolicy) {
            FileSystemItemFactory factory = ((FileSystemItemAdapterServiceImpl) Framework.getService(FileSystemItemAdapterService.class)).getFileSystemItemFactory(factoryName);
            if (!(factory instanceof VersioningFileSystemItemFactory)) {
                throw new NuxeoException(String.format("Factory %s must implement VersioningFileSystemItemFactory.",
                        factoryName));
            }
            VersioningFileSystemItemFactory versioningFactory = (VersioningFileSystemItemFactory) factory;
            FileSystemItemHelper.versionIfNeeded(versioningFactory, doc, session);
        }

        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh == null) {
            throw new NuxeoException(String.format("Document %s is not a BlobHolder, no blob can be attached to it.",
                    doc.getId()));
        }
        bh.setBlob(blob);
        session.saveDocument(doc);
        return blob;
    }

}
