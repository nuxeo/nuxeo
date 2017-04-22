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

import org.nuxeo.ecm.core.api.Blob;

/**
 * Class describing information from a {@link Blob}, suitable for serialization and storage.
 *
 * @since 7.2
 */
public class BlobInfo {

    public String key;

    public String mimeType;

    public String encoding;

    public String filename;

    public Long length;

    public String digest;

    /** Empty constructor. */
    public BlobInfo() {
    }

    /**
     * Copy constructor.
     *
     * @since 7.10
     */
    public BlobInfo(BlobInfo other) {
        key = other.key;
        mimeType = other.mimeType;
        encoding = other.encoding;
        filename = other.filename;
        length = other.length;
        digest = other.digest;
    }

}
