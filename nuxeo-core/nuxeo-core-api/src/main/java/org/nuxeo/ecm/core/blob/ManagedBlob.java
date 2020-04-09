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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.runtime.api.Framework;

/**
 * Interface for {@link Blob}s created and managed by the {@link BlobManager}.
 *
 * @since 7.2
 */
public interface ManagedBlob extends Blob {

    /**
     * Gets the id of the {@link BlobProvider} managing this blob.
     *
     * @return the blob provider id
     */
    String getProviderId();

    /**
     * Gets the stored representation of this blob.
     *
     * @return the stored representation
     */
    String getKey();

    @Override
    default InputStream getStream() throws IOException {
        BlobManager blobManager = Framework.getService(BlobManager.class);
        BlobProvider blobProvider = blobManager.getBlobProvider(this);
        if (blobProvider == null) {
            return null;
        }
        try {
            return blobProvider.getStream(this);
        } catch (IOException e) {
            // we don't want to crash everything if the remote blob cannot be accessed
            Logger log = LogManager.getLogger(ManagedBlob.class);
            log.debug(e, e);
            log.error("Failed to access file: {}", this::getKey);
            return new ByteArrayInputStream(new byte[0]);
        }
    }

    @Override
    default File getFile() {
        BlobManager blobManager = Framework.getService(BlobManager.class);
        BlobProvider blobProvider = blobManager.getBlobProvider(this);
        if (blobProvider == null) {
            return null;
        }
        return blobProvider.getFile(this);
    }

}
