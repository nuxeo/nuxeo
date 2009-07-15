/*
 * (C) Copyright 2002-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.core.api.blobholder;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.externalblob.ExternalBlobAdapter;
import org.nuxeo.ecm.core.api.model.PropertyException;

/**
 * Service interface for creating the right {@link BlobHolder} adapter depending
 * on the {@link DocumentModel} type.
 * <p>
 * Also provides APIs for external blob adapters, handling blobs that are not
 * stored in the repository (stored in the file system for instance).
 *
 * @author tiry
 * @author Anahide Tchertchian
 *
 */
public interface BlobHolderAdapterService {

    BlobHolder getBlobHolderAdapter(DocumentModel doc);

    /**
     * Returns an external blob from given uri.
     *
     * @see ExternalBlobAdapter
     * @param uri the uri describing what adapter handles the file and the
     *            needed info to retrieve it.
     * @return the resolved blob.
     * @throws PropertyException if the blob cannot be retrieved (if adapter
     *             cannot retrieve it or if file is not found for instance)
     */
    Blob getExternalBlobForUri(String uri) throws PropertyException;

    /**
     * Returns the external blob adapter registered for given prefix
     *
     * @see ExternalBlobAdapter
     */
    ExternalBlobAdapter getExternalBlobAdapterForPrefix(String prefix);

    /**
     * Returns the external blob adapter parsed from given uri
     *
     * @see ExternalBlobAdapter
     */
    ExternalBlobAdapter getExternalBlobAdapterForUri(String uri);

}
