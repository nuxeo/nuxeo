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
     * Write given metadata into given blob.
     *
     * @param blob Blob to write.
     * @param metadata Metadata to inject.
     * @return success or not.
     */
    public boolean writeMetadata(Blob blob, Map<String, Object> metadata);

    /**
     * Read from a given blob given metadata map.
     *
     * @param blob Blob to read.
     * @param metadata Metadata to extract.
     * @return Metadata map.
     */
    public Map<String, Object> readMetadata(Blob blob, List<String> metadata);

    /**
     * Read all metadata from a given blob.
     *
     * @param blob Blob to read.
     * @return Metadata map.
     */
    public Map<String, Object> readMetadata(Blob blob);


}
