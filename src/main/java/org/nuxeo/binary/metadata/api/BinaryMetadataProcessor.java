/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
     * @return success or not.
     */
    public boolean writeMetadata(Blob blob, Map<String, Object> metadata, boolean ignorePrefix);

    /**
     * Read from a given blob given metadata map. Since 7.3 ignorePrefix is added.
     *
     * @param blob Blob to read.
     * @param metadata Metadata to extract.
     * @param ignorePrefix
     * @return Metadata map.
     */
    public Map<String, Object> readMetadata(Blob blob, List<String> metadata, boolean ignorePrefix);

    /**
     * Read all metadata from a given blob. Since 7.3 ignorePrefix is added.
     *
     * @param blob Blob to read.
     * @param ignorePrefix
     * @return Metadata map.
     */
    public Map<String, Object> readMetadata(Blob blob, boolean ignorePrefix);

}
