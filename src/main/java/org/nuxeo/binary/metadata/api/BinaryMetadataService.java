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
package org.nuxeo.binary.metadata.api;

import java.util.List;
import java.util.Map;

import org.nuxeo.binary.metadata.internals.MetadataMappingDescriptor;
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
     * @param ignorePrefix Since 7.3
     * @return Extracted metadata.
     */
    public Map<String, Object> readMetadata(String processorName, Blob blob, List<String> metadataNames,
            boolean ignorePrefix);

    /**
     * Read and return metadata from a given binary and a given metadata list with Nuxeo default processor.
     *
     * @param blob Binary which metadata are read.
     * @param metadataNames Metadata list to extract from the binary.
     * @param ignorePrefix Since 7.3
     * @return Extracted metadata.
     */
    public Map<String, Object> readMetadata(Blob blob, List<String> metadataNames, boolean ignorePrefix);

    /**
     * Read and return metadata from a given binary with Nuxeo default processor.
     *
     * @param blob Binary which metadata are read.
     * @param ignorePrefix Since 7.3
     * @return Extracted metadata.
     */
    public Map<String, Object> readMetadata(Blob blob, boolean ignorePrefix);

    /**
     * Read and return metadata from a given binary with a given processor.
     *
     * @param processorName Name of the contributed processor to run.
     * @param blob Binary which metadata are read.
     * @param ignorePrefix Since 7.3
     * @return Extracted metadata.
     */
    public Map<String, Object> readMetadata(String processorName, Blob blob, boolean ignorePrefix);

    /**
     * Write given metadata into a given binary with a given processor.
     *
     * @param processorName Name of the contributed processor to run.
     * @param blob Binary which metadata are written.
     * @param metadata Injected metadata.
     * @param ignorePrefix Since 7.3
     * @return the updated blob, or {@code null} if there was an error (since 7.4)
     */
    public Blob writeMetadata(String processorName, Blob blob, Map<String, Object> metadata, boolean ignorePrefix);

    /**
     * Write given metadata into a given binary with a Nuxeo default processor.
     *
     * @param blob Binary which metadata are written.
     * @param metadata Injected metadata.
     * @param ignorePrefix Since 7.3
     * @return the updated blob, or {@code null} if there was an error (since 7.4)
     */
    public Blob writeMetadata(Blob blob, Map<String, Object> metadata, boolean ignorePrefix);

    /**
     * Write given metadata mapping id into a given binary with a Nuxeo default processor.
     *
     * @param processorName Name of the contributed processor to run.
     * @param blob Binary which metadata are written.
     * @param mappingDescriptorId The metadata mapping to apply on the document.
     * @param doc Document from properties will be read.
     * @return the updated blob, or {@code null} if there was an error (since 7.4)
     */
    public Blob writeMetadata(String processorName, Blob blob, String mappingDescriptorId, DocumentModel doc);

    /**
     * Write given metadata mapping id into a given binary with a Nuxeo default processor.
     *
     * @param blob Binary which metadata are written.
     * @param mappingDescriptorId The metadata mapping to apply on the document.
     * @param doc Document from properties will be read.
     * @return the updated blob, or {@code null} if there was an error (since 7.4)
     */
    public Blob writeMetadata(Blob blob, String mappingDescriptorId, DocumentModel doc);

    /**
     * Write metadata (from a binary) into a given Nuxeo Document according to the metadata mapping and rules
     * contributions.
     *
     * @param doc Nuxeo Document which metadata are written.
     */
    public void writeMetadata(DocumentModel doc);

    /**
     * Apply metadata mapping and override document properties according to the contribution.
     *
     * @param doc The input document.
     * @param mappingDescriptorId The metadata mapping to apply on the document.
     */
    public void writeMetadata(DocumentModel doc, String mappingDescriptorId);

    /**
     * Handle document and blob updates according to following rules in an event context: - Define if rule should be
     * executed in async or sync mode. - If Blob dirty and document metadata dirty, write metadata from doc to Blob. -
     * If Blob dirty and document metadata not dirty, write metadata from Blob to doc. - If Blob not dirty and document
     * metadata dirty, write metadata from doc to Blob.
     */
    void handleUpdate(List<MetadataMappingDescriptor> syncMappingDescriptors, DocumentModel doc);

    /**
     * Handle document and blob updates according to following rules in an event context: - Define if rule should be
     * executed in async or sync mode. - If Blob dirty and document metadata dirty, write metadata from doc to Blob. -
     * If Blob dirty and document metadata not dirty, write metadata from Blob to doc. - If Blob not dirty and document
     * metadata dirty, write metadata from doc to Blob.
     */
    void handleSyncUpdate(DocumentModel doc);

}
