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

import org.nuxeo.ecm.core.api.Blob;

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

}
