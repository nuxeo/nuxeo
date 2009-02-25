/*
 * (C) Copyright 2002-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.core.convert.cache;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;

/**
 * Cachable implementation of the {@link SimpleBlobHolder}.
 *
 * @author tiry
 */
public class SimpleCachableBlobHolder extends SimpleBlobHolder implements
        CachableBlobHolder {

    public SimpleCachableBlobHolder() {
    }

    public SimpleCachableBlobHolder(Blob blob) {
        super(blob);
    }

    public SimpleCachableBlobHolder(List<Blob> blobs) {
        super(blobs);
    }

    public SimpleCachableBlobHolder(String path) {
        super(new FileBlob(new File(path)));
    }

    public void load(String path) {
        blobs = new ArrayList<Blob>();
        File base = new File(path);
        if (base.isDirectory()) {
            File[] files = base.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    for (File subFile : file.listFiles()) {
                        Blob subBlob = new FileBlob(subFile);
                        subBlob.setFilename(subFile.getName());
                        blobs.add(subBlob);
                    }
                } else {
                    Blob mainBlob = new FileBlob(file);
                    mainBlob.setFilename(file.getName());
                    blobs.add(0, mainBlob);
                }
            }
        } else {
            File file = new File(path);
            Blob mainBlob = new FileBlob(file);
            mainBlob.setFilename(file.getName());
            blobs.add(mainBlob);
        }
    }

    public String persist(String basePath) throws Exception {
        if (blobs == null || blobs.size() == 0) {
            return null;
        }
        Path path = new Path(basePath);
        path = path.append(getHash());
        if (blobs.size() == 1) {
            File file = new File(path.toString());
            getBlob().transferTo(file);
            return file.getAbsolutePath();
        } else {
            File dir = new File(path.toString());
            dir.mkdir();
            Blob mainBlob = blobs.get(0);
            File mainFile = new File(
                    (path.append(mainBlob.getFilename())).toString());
            mainBlob.transferTo(mainFile);
            Path subDirPath = path.append("subFiles");
            File subDir = new File(subDirPath.toString());
            subDir.mkdir();
            for (int i = 1; i < blobs.size(); i++) {
                Blob blob = blobs.get(i);
                File file = new File(
                        (subDirPath.append(blob.getFilename())).toString());
                blob.transferTo(file);
            }
            return dir.getAbsolutePath();
        }
    }

}
