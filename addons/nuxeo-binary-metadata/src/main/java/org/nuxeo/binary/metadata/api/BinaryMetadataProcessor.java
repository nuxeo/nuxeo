/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.binary.metadata.api;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;

/**
 * Class to extends to contribute processor runner. See extension point 'metadataProcessors'.
 *
 * @since 7.1
 */
public interface BinaryMetadataProcessor {

    /**
     * Write given metadata into given blob. Since 7.3 ignorePrefix is added.
     *
     * @param blob Blob to write.
     * @param metadata Metadata to inject.
     * @param ignorePrefix
     * @return the updated blob, or {@code null} if there was an error (since 7.4)
     */
    Blob writeMetadata(Blob blob, Map<String, Object> metadata, boolean ignorePrefix);

    /**
     * Read from a given blob given metadata map. Since 7.3 ignorePrefix is added.
     *
     * @param blob Blob to read.
     * @param metadata Metadata to extract.
     * @param ignorePrefix
     * @return Metadata map.
     */
    Map<String, Object> readMetadata(Blob blob, List<String> metadata, boolean ignorePrefix);

    /**
     * Read all metadata from a given blob. Since 7.3 ignorePrefix is added.
     *
     * @param blob Blob to read.
     * @param ignorePrefix
     * @return Metadata map.
     */
    Map<String, Object> readMetadata(Blob blob, boolean ignorePrefix);

}
