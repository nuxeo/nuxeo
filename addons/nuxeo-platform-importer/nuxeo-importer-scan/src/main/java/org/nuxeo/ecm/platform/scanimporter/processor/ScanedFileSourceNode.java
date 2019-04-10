/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.scanimporter.processor;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.importer.source.FileSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.ecm.platform.scanimporter.service.ScanFileBlobHolder;
import org.nuxeo.ecm.platform.scanimporter.service.ScannedFileMapperService;
import org.nuxeo.runtime.api.Framework;

/**
 * Custom implementation of {@link SourceNode}. Uses XML descriptors to get meta-data and binary files location.
 *
 * @author Thierry Delprat
 */
public class ScanedFileSourceNode extends FileSourceNode {

    private static final Log log = LogFactory.getLog(ScanedFileSourceNode.class);

    protected Map<String, Serializable> properties;

    protected ScanFileBlobHolder bh;

    public static boolean useXMLMapping = true;

    public ScanedFileSourceNode(File file) {
        super(file);
    }

    public ScanedFileSourceNode(File file, ScanFileBlobHolder bh) {
        super(file);
        this.bh = bh;
    }

    public ScanedFileSourceNode(File file, Map<String, Serializable> properties) {
        super(file);
        this.properties = properties;
    }

    @Override
    public BlobHolder getBlobHolder() throws IOException {
        if (bh == null) {
            return new SimpleBlobHolder(Blobs.createBlob(file));
        } else {
            return bh;
        }
    }

    @Override
    public List<SourceNode> getChildren() throws IOException {
        List<SourceNode> children = new ArrayList<SourceNode>();

        ScannedFileMapperService sfms = Framework.getLocalService(ScannedFileMapperService.class);
        for (File child : file.listFiles()) {
            if (child.getName().endsWith(".xml") && useXMLMapping) {
                try {

                    ScanFileBlobHolder bh = sfms.parseMetaData(child);

                    if (bh != null) {
                        children.add(new ScanedFileSourceNode(child, bh));
                    } else {
                        log.error(child.getAbsolutePath() + " can not be parsed ");
                    }
                } catch (IOException e) {
                    log.error("Error during properties parsing", e);
                }
            } else if (child.isDirectory()) {
                if (child.listFiles(new DirectoryFilter()).length > 0) {
                    children.add(new ScanedFileSourceNode(child));
                } else {
                    if (useXMLMapping) {
                        if (child.list(new XmlMetaDataFileFilter()).length > 0) {
                            children.add(new ScanedFileSourceNode(child));
                        }
                    } else {
                        if (child.list().length > 0) {
                            children.add(new ScanedFileSourceNode(child));
                        }
                    }
                }
            } else if (!useXMLMapping) {
                ScanFileBlobHolder bh = new ScanFileBlobHolder(Blobs.createBlob(child), sfms.getTargetLeafType());
                children.add(new ScanedFileSourceNode(child, bh));
            }
        }
        return children;
    }

    @Override
    public String getName() {
        if (bh != null) {
            try {
                Blob blob = bh.getBlob();
                if (blob != null && blob.getFilename() != null) {
                    return blob.getFilename();
                }
            } catch (ClientException e) {
                String title = (String) bh.getProperties().get("dc:title");
                if (title != null) {
                    return title;
                }
            }
        }
        return file.getName();
    }

    public String getDescriptorFileName() {
        return file.getAbsolutePath();
    }

    private class DirectoryFilter implements FileFilter {

        @Override
        public boolean accept(File file) {
            return file.isDirectory();
        }

    }
}
