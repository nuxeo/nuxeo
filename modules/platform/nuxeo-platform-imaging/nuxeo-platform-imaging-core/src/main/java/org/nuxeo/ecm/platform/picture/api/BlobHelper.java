/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.picture.api;

import java.io.File;

import org.nuxeo.ecm.core.api.Blob;

/**
 * Helpers around Blob objects.
 *
 * @since 5.6
 */
public class BlobHelper {

    // utility class
    private BlobHelper() {
    }

    /**
     * If the blob is backed by an actual file, return it.
     * <p>
     * Note that the File may be short-lived (temporary file), so should be used immediately.
     *
     * @return a File, or {@code null} if this blob doesn't have one
     * @deprecated since 7.2, use {@link Blob#getFile} directly
     */
    @Deprecated
    public static File getFileFromBlob(Blob blob) {
        return blob.getFile();
    }

}
