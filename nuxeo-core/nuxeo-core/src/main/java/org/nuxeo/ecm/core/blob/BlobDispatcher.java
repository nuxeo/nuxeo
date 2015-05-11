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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob;

import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.BlobManager.BlobInfo;
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
     * @param blob the blob
     * @param doc the document containing the blob
     * @return the blob provider id and whether it should be added as prefix
     */
    BlobDispatch getBlobProvider(Blob blob, Document doc);

}
