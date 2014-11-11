/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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


    @Override
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
                prefix = prefix + file.getName() + File.separatorChar;
                addDirectoryToList(file, prefix);
                prefix = prefix.substring(0, beginIndex);
            } else {
                Blob blob = new FileBlob(file);
                blob.setFilename(prefix + file.getName());
                if (file.getName().equalsIgnoreCase("index.html")) {
                    blobs.add(0, blob);
                } else {
                    blobs.add(blob);
                }
            }
        }
    }

    @Override
    public String persist(String basePath) throws Exception {
        if (blobs == null || blobs.isEmpty()) {
            return null;
        }
        Path path = new Path(basePath);
        path = path.append(getHash());
        if (blobs.size() == 1) {
            File file = new File(path.toString());
            getBlob().transferToOrMove(file, true);
            return file.getAbsolutePath();
        } else {
            File dir = new File(path.toString());
            dir.mkdir();
            for (Blob blob : blobs) {
                File file = new File(path.append(blob.getFilename()).toString());
                blob.transferToOrMove(file, true);
            }
            return dir.getAbsolutePath();
        }
    }

}
