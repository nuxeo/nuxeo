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
import org.nuxeo.ecm.core.convert.api.ConversionException;

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
        try {
            if (base.isDirectory()) {
                addDirectoryToList(base, "");
            } else {
                File file = new File(path);
                Blob mainBlob = new FileBlob(file);
                mainBlob.setFilename(file.getName());
                blobs.add(mainBlob);
            }
        } catch (Exception e) {
            throw new RuntimeException("Blob loading from cache failed",
                    e.getCause());
        }
    }

    public void addDirectoryToList(File directory, String prefix)
            throws ConversionException {
        File[] directoryContent = directory.listFiles();
        for (File file : directoryContent) {
            if (file.isDirectory()) {
                int beginIndex = prefix.length();
                prefix = prefix.concat(file.getName() + File.separatorChar);
                addDirectoryToList(file, prefix);
                prefix = prefix.substring(0, beginIndex);
            } else {
                Blob blob = new FileBlob(file);
                blob.setFilename(prefix.concat(file.getName()));
                if (file.getName().equalsIgnoreCase("index.html")) {
                    blobs.add(0, blob);
                } else {
                    blobs.add(blob);
                }
            }
        }
    }

    public String persist(String basePath) throws Exception {
        if (blobs == null || blobs.isEmpty()) {
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
            for (Blob blob : blobs) {
                File file = new File((path.append(blob.getFilename())).toString());
                blob.transferTo(file);
            }
            return dir.getAbsolutePath();
        }
    }

}
