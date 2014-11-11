/*
 * (C) Copyright 2002-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.convert.plugins;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.utils.Path;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.platform.mimetype.MimetypeDetectionException;
import org.nuxeo.ecm.platform.mimetype.MimetypeNotFoundException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;

/**
 * Cachable implementation of a zip file.
 *
 * @author Laurent Doguin
 */
public class ZipCachableBlobHolder extends SimpleCachableBlobHolder {

    protected Blob zipBlob;

    protected MimetypeRegistry mimeTypeService;

    protected String key;

    public ZipCachableBlobHolder() {
    }

    public ZipCachableBlobHolder(Blob zipBlob) {
        this.zipBlob = zipBlob;
    }

    public Blob getBlob(String path) throws IOException,
            MimetypeNotFoundException, MimetypeDetectionException,
            ConversionException {
        String filePath = key + path;
        File file = new File(filePath);
        Blob blob = new FileBlob(file);
        String mimeType = getMimeTypeService().getMimetypeFromBlob(blob);
        blob.setMimeType(mimeType);
        blob.setFilename(path);
        return blob;
    }

    @Override
    public Blob getBlob() throws ClientException {
        return zipBlob;
    }

    @Override
    public List<Blob> getBlobs() throws ClientException {
        if (blobs == null) {
            load(key);
        }
        return blobs;
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
                String mimeType = getMimeType(file);
                Blob mainBlob = new FileBlob(file);
                mainBlob.setMimeType(mimeType);
                mainBlob.setFilename(file.getName());
                blobs.add(mainBlob);
            }
        } catch (Exception e) {
            throw new RuntimeException("Blob loading from cache failed",
                    e.getCause());
        }
    }

    @Override
    public void addDirectoryToList(File directory, String prefix)
            throws ConversionException {
        File[] directoryContent = directory.listFiles();
        for (File file : directoryContent) {
            if (file.isDirectory()) {
                int beginIndex = prefix.length();
                prefix += file.getName() + File.separatorChar;
                addDirectoryToList(file, prefix);
                prefix = prefix.substring(0, beginIndex);
            } else {
                Blob blob = new FileBlob(file);
                String mimeType = getMimeType(file);
                blob.setMimeType(mimeType);
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
        Path path = new Path(basePath);
        path = path.append(getHash());
        File dir = new File(path.toString());
        dir.mkdir();
        ZipUtils.unzip(zipBlob.getStream(), dir);
        key = dir.getAbsolutePath();
        return key;
    }

    public String getMimeType(File file) throws ConversionException{
        try {
            return getMimeTypeService().getMimetypeFromFile(file);
        } catch (ConversionException e) {
            throw new ConversionException("Could not get MimeTypeRegistry", e);
        } catch (MimetypeNotFoundException e) {
            return "application/octet-stream";
        } catch (MimetypeDetectionException e) {
            return "application/octet-stream";
        }
    }

    public MimetypeRegistry getMimeTypeService() throws ConversionException {
        if (mimeTypeService == null) {
            try {
                mimeTypeService = Framework.getService(MimetypeRegistry.class);
            } catch (Exception e) {
                throw new ConversionException("Could not get MimeTypeRegistry", e);
            }
        }
        return mimeTypeService;
    }
}
