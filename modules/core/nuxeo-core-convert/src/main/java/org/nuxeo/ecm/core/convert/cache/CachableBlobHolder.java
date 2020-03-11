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
package org.nuxeo.ecm.core.convert.cache;

import java.io.IOException;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

/**
 * Extended interface for {@link BlobHolder} that can be cached.
 * <p>
 * A BlobHolder can be cached if it can be persisted to disk and reloaded from a file. Converters need to return
 * BlobHolders that implement this interface to make the result cachable.
 *
 * @author tiry
 */
public interface CachableBlobHolder extends BlobHolder {

    /**
     * Persists the blobHolder to disk.
     *
     * @param basePath the base path (existing directory) as determined by the caller
     * @return the full path of the newly created FileSystem resource
     */
    String persist(String basePath) throws IOException;

    /**
     * Reloads the {@link BlobHolder} from a file.
     *
     * @param path
     */
    void load(String path) throws IOException;

}
