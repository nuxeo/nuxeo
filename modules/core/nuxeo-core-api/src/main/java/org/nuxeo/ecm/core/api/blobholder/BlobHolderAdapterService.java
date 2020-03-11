/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.core.api.blobholder;

import java.io.IOException;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.externalblob.ExternalBlobAdapter;

/**
 * Service interface for creating the right {@link BlobHolder} adapter depending on the {@link DocumentModel} type.
 * <p>
 * Also provides APIs for external blob adapters, handling blobs that are not stored in the repository (stored in the
 * file system for instance).
 *
 * @author tiry
 * @author Anahide Tchertchian
 */
public interface BlobHolderAdapterService {

    BlobHolder getBlobHolderAdapter(DocumentModel doc);

    /**
     * Get a blob holder adapter instantiated by given factory name.
     *
     * @param factoryName the factory name
     * @return a blob holder adapter
     * @since 9.3
     */
    BlobHolder getBlobHolderAdapter(DocumentModel doc, String factoryName);

    /**
     * Returns an external blob from given uri.
     *
     * @see ExternalBlobAdapter
     * @param uri the uri describing what adapter handles the file and the needed info to retrieve it.
     * @return the resolved blob.
     * @throws PropertyException if the blob cannot be retrieved (if adapter cannot retrieve it or if file is not found
     *             for instance)
     */
    Blob getExternalBlobForUri(String uri) throws PropertyException, IOException;

    /**
     * Returns the external blob adapter registered for given prefix.
     *
     * @see ExternalBlobAdapter
     */
    ExternalBlobAdapter getExternalBlobAdapterForPrefix(String prefix);

    /**
     * Returns the external blob adapter parsed from given URI.
     *
     * @see ExternalBlobAdapter
     */
    ExternalBlobAdapter getExternalBlobAdapterForUri(String uri);

}
