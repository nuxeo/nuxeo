/*
 * (C) Copyright 2015-2017 Nuxeo (http://nuxeo.com/) and others.
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

import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.model.Document;

/**
 * Interface for a provider of {@link Blob}s interacting with {@link Document}s.
 *
 * @since 9.2
 */
public interface DocumentBlobProvider {

    /**
     * Returns a new managed blob pointing to a fixed version of the original blob.
     * <p>
     *
     * @param blob the original managed blob
     * @param doc the document that holds the blob
     * @return a managed blob with fixed version, or {@code null} if no change is needed
     * @since 7.3
     */
    default ManagedBlob freezeVersion(ManagedBlob blob, Document doc) throws IOException {
        return null;
    }

    /**
     * Gets an {@link InputStream} for a conversion to the given MIME type.
     * <p>
     * Like all {@link InputStream}, the result must be closed when done with it to avoid resource leaks.
     *
     * @param blob the managed blob
     * @param mimeType the MIME type to convert to
     * @param doc the document that holds the blob
     * @return the stream, or {@code null} if no conversion is available for the given MIME type
     * @since 7.3
     */
    default InputStream getConvertedStream(ManagedBlob blob, String mimeType, DocumentModel doc) throws IOException {
        return null;
    }

}
