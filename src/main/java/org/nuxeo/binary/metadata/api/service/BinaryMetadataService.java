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
 *     vpasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.binary.metadata.api.service;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Read/Write binary metadata services.
 *
 * @since 7.1
 */
public interface BinaryMetadataService {

    /**
     * Read and return metadata from a given binary and a given metadata list with a given processor.
     *
     * @param processorName Name of the contributed processor to run.
     * @param blob Binary which metadata are read.
     * @param metadataNames Metadata list to extract from the binary.
     * @return Extracted metadata.
     */
    public Map<String, Object> readMetadata(String processorName, Blob blob, List<String> metadataNames);

    /**
     * Read and return metadata from a given binary and a given metadata list with Nuxeo default processor.
     *
     * @param blob Binary which metadata are read.
     * @param metadataNames Metadata list to extract from the binary.
     * @return Extracted metadata.
     */
    public Map<String, Object> readMetadata(Blob blob, List<String> metadataNames);

    /**
     * Read and return metadata from a given binary with Nuxeo default processor.
     *
     * @param blob Binary which metadata are read.
     * @return Extracted metadata.
     */
    public Map<String, Object> readMetadata(Blob blob);

    /**
     * Read and return metadata from a given binary with a given processor.
     *
     * @param processorName Name of the contributed processor to run.
     * @param blob Binary which metadata are read.
     * @return Extracted metadata.
     */
    public Map<String, Object> readMetadata(String processorName, Blob blob);

    /**
     * Write metadata into a given binary with a given processor.
     *
     * @param processorName Name of the contributed processor to run.
     * @param blob Binary which metadata are written.
     * @param metadata Injected metadata.
     * @return success or not.
     */
    public boolean writeMetadata(String processorName, Blob blob, Map<String, Object> metadata);

    /**
     * Write metadata into a given binary with a Nuxeo default processor.
     *
     * @param blob Binary which metadata are written.
     * @param metadata Injected metadata.
     * @return success or not.
     */
    public boolean writeMetadata(Blob blob, Map<String, Object> metadata);

    /**
     * Write metadata (from a binary) into a given Nuxeo Document according to the metadata mapping and rules contributions.
     *
     * @param doc Nuxeo Document which metadata are written.
     */
    public void writeMetadata(DocumentModel doc);
}
