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

    public static final String PROPERTY_FILE_SUFIX = ".properties";

    protected static IndividualMetadataCollector collector = new IndividualMetadataCollector();

    public FileWithIndividualMetadasSourceNode(File file) {
        super(file);
    }

    public FileWithIndividualMetadasSourceNode(String path) {
        super(path);
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
        log.trace("Element " + this.getSourcePath() + " has children" + listFiles.toString());
        for (File child : listFiles) {
            if (isPropertyFile(child)) {
                try {
                    String propKey = getPropertyTargetKey(child, listFiles);
                    if (propKey != null) {
                        collector.addPropertyFile(child, propKey);
                    }
                } catch (IOException e) {
                    log.error("Error during properties parsing", e);
                }
            } else {
                children.add(new FileWithIndividualMetadasSourceNode(child));
            }
        }
        return children;
    }

    protected boolean isPropertyFile(File file) {
        return (file.getName().contains(PROPERTY_FILE_SUFIX));
    }

    protected String getPropertyTargetKey(File propFile, File[] listFiles) {
        String fileName = propFile.getName();
        String absFileName = fileName.substring(0, fileName.lastIndexOf(PROPERTY_FILE_SUFIX));
        for (File file2 : listFiles) {
            if (file2.isDirectory() && file2.getName().equals(absFileName)) {
                return file2.getAbsolutePath();
            }
        }
        for (File file2 : listFiles) {
            if (file2.isFile() && !isPropertyFile(file2) && getFileNameNoExt(file2).equals(absFileName)) {
                return file2.getAbsolutePath();
            }
        }
        return null;
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