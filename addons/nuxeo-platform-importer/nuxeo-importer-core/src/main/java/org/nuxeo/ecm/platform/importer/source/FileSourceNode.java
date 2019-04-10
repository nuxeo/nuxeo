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
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;

/**
 * Simple Filesystem based {@link SourceNode}
 *
 * @author Thierry Delprat
 */
public class FileSourceNode implements SourceNode {

    protected File file;

    public FileSourceNode(File file) {
        this.file = file;
    }

    public FileSourceNode(String path) {
        this.file = new File(path);
    }

    public BlobHolder getBlobHolder() throws IOException {
        return new SimpleBlobHolder(Blobs.createBlob(file));
    }

    public List<SourceNode> getChildren() throws IOException {

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

    public String getSourcePath() {
        return file.getAbsolutePath();
    }

    public File getFile() {
        return file;
    }

    public static String getFileNameNoExt(File file) {
        String name = file.getName();
        int p = name.lastIndexOf('.');
        if (p == -1) {
            return name;
        }
        return name.substring(0, p);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(file);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        file = (File) in.readObject();
    }
}
