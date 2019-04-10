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

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;

/**
 *
 * Simple Filesystem based {@link SourceNode}
 *
 * @author Thierry Delprat
 *
 */
public class FileSourceNode implements SourceNode {

    protected File file;

    public FileSourceNode(File file) {
        this.file = file;
    }

    public FileSourceNode(String path) {
        this.file = new File(path);
    }

    public BlobHolder getBlobHolder() {
        return new SimpleBlobHolder(new FileBlob(file));
    }

    public List<SourceNode> getChildren() {

        List<SourceNode> children = new ArrayList<SourceNode>();

        for (File child : file.listFiles()) {
            children.add(new FileSourceNode(child));
        }
        return children;
    }

    public boolean isFolderish() {
        return file.isDirectory();
    }

    public String getName() {
        return file.getName();
    }
    
    public String getSourcePath(){
       return file.getAbsolutePath();
    }

    public File getFile() {
        return file;
    }

}
