/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.importer.source;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolderWithProperties;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.importer.properties.MetadataCollector;

/**
 *
 * {@link SourceNode} implementation that manages Meta-data from properties
 * files
 *
 * @author Thierry Delprat
 *
 */
public class FileWithMetadataSourceNode extends FileSourceNode {

    protected static MetadataCollector collector = new MetadataCollector();

    public static String METADATA_FILENAME = "metadata.properties";

    private static final Log log = LogFactory.getLog(FileWithMetadataSourceNode.class);

    public FileWithMetadataSourceNode(File file) {
        super(file);
    }

    @Override
    public BlobHolder getBlobHolder() {
        BlobHolder bh = new SimpleBlobHolderWithProperties(new FileBlob(file),
                collector.getProperties(file.getPath()));
        return bh;
    }

    @Override
    public List<SourceNode> getChildren() {

        List<SourceNode> children = new ArrayList<SourceNode>();

        for (File child : file.listFiles()) {
            if (METADATA_FILENAME.equals(child.getName())) {
                try {
                    collector.addPropertyFile(child);
                } catch (Exception e) {
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
