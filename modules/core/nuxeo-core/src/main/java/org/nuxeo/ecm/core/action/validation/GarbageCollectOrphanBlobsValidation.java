/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.core.action.validation;

import static javax.servlet.http.HttpServletResponse.SC_NOT_IMPLEMENTED;
import static org.nuxeo.ecm.core.action.GarbageCollectOrphanBlobsAction.DRY_RUN_PARAM;

import java.util.List;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.BlobStoreBlobProvider;
import org.nuxeo.ecm.core.blob.DocumentBlobManager;
import org.nuxeo.ecm.core.bulk.AbstractBulkActionValidation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 2023
 */
public class GarbageCollectOrphanBlobsValidation extends AbstractBulkActionValidation {

    @Override
    protected List<String> getParametersToValidate() {
        return List.of(DRY_RUN_PARAM);
    }

    @Override
    protected void validateCommand(BulkCommand command) throws IllegalArgumentException {
        validateBoolean(DRY_RUN_PARAM, command);
        String repositoryName = command.getRepository();
        BlobManager blobManager = Framework.getService(BlobManager.class);
        DocumentBlobManager documentBlobManager = Framework.getService(DocumentBlobManager.class);
        try {
            documentBlobManager.checkCanDeleteBlob(command.getRepository());
            for (String providerId : documentBlobManager.getProviderIds(repositoryName)) {
                BlobProvider provider = blobManager.getBlobProvider(providerId);
                if (!(provider instanceof BlobStoreBlobProvider)) {
                    throw new UnsupportedOperationException("Provider " + providerId + " of class "
                            + provider.getClass().getCanonicalName() + " does not extend BlobStoreBlobProvider");
                }
            }
        } catch (UnsupportedOperationException e) {
            throw new NuxeoException(e, SC_NOT_IMPLEMENTED);
        }
    }

}
