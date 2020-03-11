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
import java.util.Calendar;
import java.util.Set;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.blob.binary.BinaryManagerStatus;
import org.nuxeo.ecm.core.model.Document;

/**
 * Service managing {@link Blob}s associated to a {@link Document} or a repository.
 *
 * @since 9.2
 */
public interface DocumentBlobManager {

    /**
     * Reads a {@link Blob} from storage.
     *
     * @param blobInfo the blob information
     * @param repositoryName the repository to which this blob belongs
     * @return a managed blob
     * @since 7.2
     */
    Blob readBlob(BlobInfo blobInfo, String repositoryName) throws IOException;

    /**
     * Writes a {@link Blob} to storage and returns its key.
     *
     * @param blob the blob
     * @param doc the document to which this blob belongs
     * @param xpath the xpath of blob in doc
     * @return the blob key
     * @since 9.1
     */
    String writeBlob(Blob blob, Document doc, String xpath) throws IOException;

    /**
     * Gets an {@link InputStream} for a conversion to the given MIME type.
     * <p>
     * Like all {@link InputStream}, the result must be closed when done with it to avoid resource leaks.
     *
     * @param blob the blob
     * @param mimeType the MIME type to convert to
     * @param doc the document that holds the blob
     * @return the stream, or {@code null} if no conversion is available for the given MIME type
     * @since 7.2
     */
    InputStream getConvertedStream(Blob blob, String mimeType, DocumentModel doc) throws IOException;

    /**
     * Freezes the blobs' versions on a document version when it is created via a check in.
     *
     * @param doc the new document version
     * @since 7.3
     */
    void freezeVersion(Document doc);

    /**
     * Notifies the blob manager that a set of xpaths have changed on a document.
     *
     * @param doc the document
     * @param xpaths the set of changed xpaths
     * @since 7.3
     */
    void notifyChanges(Document doc, Set<String> xpaths);

    /**
     * Notifies the blob manager that the document was made a record.
     *
     * @param doc the document
     * @since 11.1
     */
    void notifyMakeRecord(Document doc);

    /**
     * Notifies the blob manager that the document has been copied.
     *
     * @param doc the new document, the result of the copy
     * @since 11.1
     */
    void notifyAfterCopy(Document doc);

    /**
     * Notifies the blob manager that the document is about to be removed.
     *
     * @param doc the document
     * @since 11.1
     */
    void notifyBeforeRemove(Document doc);

    /**
     * Notifies the blob manager that the document's retention date was changed.
     *
     * @param doc the document
     * @since 11.1
     */
    void notifySetRetainUntil(Document doc, Calendar retainUntil);

    /**
     * Notifies the blob manager that the document's legal hold status was changed.
     *
     * @param doc the document
     * @since 11.1
     */
    void notifySetLegalHold(Document doc, boolean hold);

    /**
     * Garbage collect the unused binaries.
     *
     * @param delete if {@code false} don't actually delete the garbage collected binaries (but still return statistics
     *            about them), if {@code true} delete them
     * @return a status about the number of garbage collected binaries
     * @since 7.4
     */
    BinaryManagerStatus garbageCollectBinaries(boolean delete);

    /**
     * Checks if a garbage collection of the binaries in progress.
     *
     * @return {@code true} if a garbage collection of the binaries is in progress
     * @since 7.4
     */
    boolean isBinariesGarbageCollectionInProgress();

    /**
     * INTERNAL. Marks a binary as referenced during garbage collection. Called back by repository implementations
     * during {@link #garbageCollectBinaries}.
     *
     * @param key the binary key
     * @param repositoryName the repository name
     * @since 7.4
     */
    void markReferencedBinary(String key, String repositoryName);

}
