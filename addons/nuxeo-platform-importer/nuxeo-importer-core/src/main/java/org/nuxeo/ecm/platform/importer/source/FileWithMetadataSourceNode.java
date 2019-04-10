/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.importer.source;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolderWithProperties;
import org.nuxeo.ecm.platform.importer.properties.MetadataCollector;

/**
 * {@link SourceNode} implementation that manages Meta-data from properties files
 *
 * @author Thierry Delprat
 */
public class FileWithMetadataSourceNode extends FileSourceNode {

    protected static MetadataCollector collector = new MetadataCollector();

    public static final String METADATA_FILENAME = "metadata.properties";

    private static final Log log = LogFactory.getLog(FileWithMetadataSourceNode.class);

    public FileWithMetadataSourceNode(File file) {
        super(file);
    }

    public FileWithMetadataSourceNode(String path) {
        super(path);
    }

    @Override
    public BlobHolder getBlobHolder() throws IOException {
        BlobHolder bh = new SimpleBlobHolderWithProperties(Blobs.createBlob(file), collector.getProperties(file.getPath()));
        return bh;
    }

    @Override
    public List<SourceNode> getChildren() {

        List<SourceNode> children = new ArrayList<SourceNode>();

        for (File child : file.listFiles()) {
            if (METADATA_FILENAME.equals(child.getName())) {
                try {
                    collector.addPropertyFile(child);
                } catch (IOException e) {
                    log.error("Error during properties parsing", e);
                }
                break;
            }
        }

        for (File child : file.listFiles()) {
            if (!METADATA_FILENAME.equals(child.getName())) {
                children.add(new FileWithMetadataSourceNode(child));
            }
        }
        return children;
    }

}
