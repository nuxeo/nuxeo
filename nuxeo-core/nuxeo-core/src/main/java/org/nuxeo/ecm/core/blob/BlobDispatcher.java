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

import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.model.Document;

/**
 * Interface for a dispatcher of blobs to different blob providers according to metadata.
 *
 * @since 7.3
 */
public interface BlobDispatcher {

    static final class BlobDispatch {
        public final String providerId;

        public final boolean addPrefix;

        public BlobDispatch(String providerId, boolean addPrefix) {
            this.providerId = providerId;
            this.addPrefix = addPrefix;
        }
    }

    /**
     * Initializes this blob dispatcher.
     */
    void initialize(Map<String, String> properties);

    /**
     * Gets the provider ids to which this dispatcher can dispatch.
     * <p>
     * Blobs already having a provider id not listed here won't be touched on write.
     *
     * @return a collection containing the provider ids
     */
    Collection<String> getBlobProviderIds();

    /**
     * Decides which {@link BlobProvider} to use to read a blob from the given repository if no prefix is specified in
     * the blob key.
     *
     * @param repositoryName the repository name
     * @return the blob provider id
     */
    String getBlobProvider(String repositoryName);

    /**
     * Decides which {@link BlobProvider} to use to write the given blob, and whether the provider id should be added as
     * prefix to the managed blob key.
     *
     * @param doc the document containing the blob
     * @param blob the blob
     * @return the blob provider id and whether it should be added as prefix
     * @deprecated since 9.1, use {@link #getBlobProvider(Document, Blob, String)} instead
     */
    @Deprecated
    default BlobDispatch getBlobProvider(Document doc, Blob blob) {
        return getBlobProvider(doc, blob, null);
    }

    /**
     * Decides which {@link BlobProvider} to use to write the given blob, and whether the provider id should be added as
     * prefix to the managed blob key.
     *
     * @param doc the document containing the blob
     * @param blob the blob
     * @param xpath the xpath of the blob in the document
     * @return the blob provider id and whether it should be added as prefix
     * @since 9.1
     */
    BlobDispatch getBlobProvider(Document doc, Blob blob, String xpath);

    /**
     * Notifies the blob dispatcher that a set of xpaths have changed on a document.
     *
     * @param doc the document
     * @param xpaths the set of changed xpaths
     * @since 7.3
     */
    void notifyChanges(Document doc, Set<String> xpaths);

    /**
     * Notifies the blob dispatcher that the document was made a record.
     *
     * @param doc the document
     * @since 11.1
     */
    default void notifyMakeRecord(Document doc) {
        // do nothing, for forward compatibility of non-default implementations
    }

    /**
     * Notifies the blob dispatcher that the document has been copied.
     *
     * @param doc the new document, the result of the copy
     * @since 11.1
     */
    default void notifyAfterCopy(Document doc) {
        // do nothing, for forward compatibility of non-default implementations
    }

    /**
     * Notifies the blob dispatcher that the document is about to be removed.
     *
     * @param doc the document
     * @since 11.1
     */
    default void notifyBeforeRemove(Document doc) {
        // do nothing, for forward compatibility of non-default implementations
    }

}
