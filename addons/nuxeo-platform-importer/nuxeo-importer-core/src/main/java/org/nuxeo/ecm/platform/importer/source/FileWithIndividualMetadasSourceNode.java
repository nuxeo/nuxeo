/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    private static final Log log = LogFactory.getLog(FileWithIndividualMetadasSourceNode.class);

    public static final String PROPERTY_FILE_SUFFIX = ".properties";

    /** @deprecated misspelled */
    @Deprecated
    public static final String PROPERTY_FILE_SUFIX = PROPERTY_FILE_SUFFIX;

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
        List<SourceNode> children = new ArrayList<SourceNode>();
        File[] listFiles = file.listFiles();
        log.trace("Element " + this.getSourcePath() + " has " + listFiles.length + " children");
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
                        log.error("Error during properties parsing for: " + child, e);
                    }
                }
            } else {
                children.add(newInstance(child));
            }
        }
        return children;
    }

    /** @deprecated unused. */
    @Deprecated
    protected boolean isPropertyFile(File file) {
        return file.getName().endsWith(PROPERTY_FILE_SUFFIX);
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
