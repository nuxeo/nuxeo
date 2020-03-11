/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.convert.cache;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;

/**
 * Cachable implementation of the {@link SimpleBlobHolder}.
 *
 * @author tiry
 */
public class SimpleCachableBlobHolder extends SimpleBlobHolder implements CachableBlobHolder {

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
    public void load(String path) throws IOException {
        blobs = new ArrayList<>();
        File base = new File(path);
        if (base.isDirectory()) {
            addDirectoryToList(base, "");
        } else {
            File file = new File(path);
            Blob mainBlob = Blobs.createBlob(file);
            mainBlob.setFilename(file.getName());
            blobs.add(mainBlob);
        }

        orderIndexPageFirst(blobs);
    }

    public void addDirectoryToList(File directory, String prefix) throws IOException {
        File[] directoryContent = directory.listFiles();
        for (File file : directoryContent) {
            if (file.isDirectory()) {
                int beginIndex = prefix.length();
                prefix = prefix + file.getName() + File.separatorChar;
                addDirectoryToList(file, prefix);
                prefix = prefix.substring(0, beginIndex);
            } else {
                Blob blob = Blobs.createBlob(file);
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
    public String persist(String basePath) throws IOException {
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
                File file = new File(path.append(blob.getFilename()).toString());
                blob.transferTo(file);
            }
            return dir.getAbsolutePath();
        }
    }

    /**
     * Rearrange blobs to have smallest index.html page as the first blob.
     *
     * @since 5.7.1
     */
    protected void orderIndexPageFirst(List<Blob> blobs) {
        Blob indexPage = null;
        for (Blob blob : blobs) {
            if (blob.getFilename().contains("index.html")
                    && (indexPage == null || blob.getFilename().compareTo(indexPage.getFilename()) < 0)) {
                indexPage = blob;
            }
        }

        if (indexPage != null) {
            blobs.remove(indexPage);
            blobs.add(0, indexPage);
        }
    }

}
