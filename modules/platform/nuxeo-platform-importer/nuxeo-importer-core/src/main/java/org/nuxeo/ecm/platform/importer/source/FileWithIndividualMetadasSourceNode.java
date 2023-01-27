/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *    Mariana Cedica
 */
package org.nuxeo.ecm.platform.importer.source;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolderWithProperties;
import org.nuxeo.ecm.platform.importer.properties.IndividualMetadataCollector;

/**
 * {@link SourceNode} implementation that manages Meta-data from properties files per document
 * <p>
 * The properties are mapped by the collector using as key the path of the file/folder to import.
 */
public class FileWithIndividualMetadasSourceNode extends FileSourceNode {

    private static final Logger log = LogManager.getLogger(FileWithIndividualMetadasSourceNode.class);

    public static final String PROPERTY_FILE_SUFFIX = ".properties";

    protected static IndividualMetadataCollector collector = new IndividualMetadataCollector();

    public FileWithIndividualMetadasSourceNode(File file) {
        super(file);
    }

    public FileWithIndividualMetadasSourceNode(String path) {
        super(path);
    }

    /* convenience factory to easy subclassing */
    protected FileWithIndividualMetadasSourceNode newInstance(File file) {
        return new FileWithIndividualMetadasSourceNode(file);
    }

    @Override
    public BlobHolder getBlobHolder() throws IOException {
        BlobHolder bh = new SimpleBlobHolderWithProperties(Blobs.createBlob(file), collector.getProperties(file));
        return bh;
    }

    public Map<String, Serializable> getMetadataForFolderishNode() {
        return collector.getProperties(file);
    }

    @Override
    public List<SourceNode> getChildren() {
        List<SourceNode> children = new ArrayList<>();
        File[] listFiles = file.listFiles();
        log.trace("Element {} has {} children", this::getSourcePath, () -> listFiles.length);
        // compute map from base name without extension to absolute path
        Map<String, String> paths = new HashMap<>();
        for (File child : listFiles) {
            String name = child.getName();
            if (child.isDirectory()) {
                paths.put(name, child.getAbsolutePath());
            } else if (child.isFile() && !name.endsWith(PROPERTY_FILE_SUFFIX)) {
                String base = getFileNameNoExt(child);
                paths.put(base, child.getAbsolutePath());
            }
        }
        for (File child : listFiles) {
            String name = child.getName();
            if (name.endsWith(PROPERTY_FILE_SUFFIX)) {
                String base = name.substring(0, name.length() - PROPERTY_FILE_SUFFIX.length());
                String path = paths.get(base);
                if (path != null) {
                    try {
                        collector.addPropertyFile(child, path);
                    } catch (IOException e) {
                        log.error("Error during properties parsing for: {}", child, e);
                    }
                }
            } else {
                children.add(newInstance(child));
            }
        }
        return children;
    }

    public static String getFileNameNoExt(File file) {
        String name = file.getName();
        int p = name.lastIndexOf('.');
        if (p == -1) {
            return name;
        }
        return name.substring(0, p);
    }
}
