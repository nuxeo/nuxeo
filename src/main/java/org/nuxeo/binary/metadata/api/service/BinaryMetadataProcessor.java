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
package org.nuxeo.binary.metadata.api.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLBlob;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.streaming.FileSource;
import org.nuxeo.runtime.services.streaming.StreamSource;

/**
 * Class to extends to contribute processor runner. See extension point 'metadataProcessors'.
 *
 * @since 7.1
 */
public abstract class BinaryMetadataProcessor {

    protected final CommandLineExecutorService commandLineExecutorService;

    protected BinaryMetadataProcessor() {
        this.commandLineExecutorService = Framework.getService(CommandLineExecutorService.class);
    }

    /**
     * Write given metadata into given blob.
     *
     * @param blob Blob to write.
     * @param metadata Metadata to inject.
     * @return success or not.
     */
    public abstract boolean writeMetadata(Blob blob, Map<String, String> metadata);

    /**
     * Read from a given blob given metadata map.
     *
     * @param blob Blob to read.
     * @param metadata Metadata to extract.
     * @return Metadata map.
     */
    public abstract Map<String, Object> readMetadata(Blob blob, List<String> metadata);

    /**
     * Read all metadata from a given blob.
     *
     * @param blob Blob to read.
     * @return Metadata map.
     */
    public abstract Map<String, Object> readMetadata(Blob blob);

    public CommandLineExecutorService getCommandLineExecutorService() {
        return commandLineExecutorService;
    }

    /*--------------------- Utils -----------------------*/

    public File makeFile(Blob blob) throws IOException {
        File sourceFile = getFileFromBlob(blob);
        if (sourceFile == null) {
            String filename = blob.getFilename();
            sourceFile = File.createTempFile(filename, ".tmp");
            blob.transferTo(sourceFile);
            Framework.trackFile(sourceFile, this);
        }
        return sourceFile;
    }

    public File getFileFromBlob(Blob blob) {
        if (blob instanceof FileBlob) {
            return ((FileBlob) blob).getFile();
        } else if (blob instanceof SQLBlob) {
            StreamSource source = ((SQLBlob) blob).getBinary().getStreamSource();
            return ((FileSource) source).getFile();
        }
        return null;
    }
}
