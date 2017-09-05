/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat <tdelprat@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */

package org.nuxeo.ecm.core.transientstore;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.transientstore.api.MaximumTransientSpaceExceeded;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreConfig;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreProvider;

/**
 * Base class for a {@link TransientStore} implementation.
 *
 * @since 7.2
 */
public abstract class AbstractTransientStore implements TransientStoreProvider {

    protected static final Log log = LogFactory.getLog(AbstractTransientStore.class);

    protected TransientStoreConfig config;

    protected File cacheDir;

    @Override
    public void init(TransientStoreConfig config) {
        this.config = config;
        File data = getDataDir(config);
        data.mkdirs();
        cacheDir = data.getAbsoluteFile();
    }

    private File getDataDir(TransientStoreConfig config) {
        String dataDirPath = config.getDataDir();
        if (StringUtils.isBlank(dataDirPath)) {
            File transienStoreHome = new File(Environment.getDefault().getData(), "transientstores");
            return new File(transienStoreHome, config.getName());
        } else {
            return new File(dataDirPath);
        }
    }

    @Override
    public abstract void shutdown();

    @Override
    public abstract boolean exists(String key);

    @Override
    public abstract Set<String> keySet();

    @Override
    public abstract void putParameter(String key, String parameter, Serializable value);

    @Override
    public abstract Serializable getParameter(String key, String parameter);

    @Override
    public abstract void putParameters(String key, Map<String, Serializable> parameters);

    @Override
    public abstract Map<String, Serializable> getParameters(String key);

    @Override
    public abstract List<Blob> getBlobs(String key);

    @Override
    public abstract long getSize(String key);

    @Override
    public abstract boolean isCompleted(String key);

    @Override
    public abstract void setCompleted(String key, boolean completed);

    @Override
    public abstract void remove(String key);

    @Override
    public abstract void release(String key);

    /**
     * Updates the total storage size and the storage size of the entry with the given {@code key} according to
     * {@code sizeOfBlobs} and stores the blob information in this entry.
     */
    protected abstract void persistBlobs(String key, long sizeOfBlobs, List<Map<String, String>> blobInfos);

    /**
     * Sets the size of the disk storage in bytes.
     */
    protected abstract void setStorageSize(long newSize);

    protected abstract long incrementStorageSize(long size);

    protected abstract long decrementStorageSize(long size);

    protected abstract void removeAllEntries();

    @Override
    public void putBlobs(String key, List<Blob> blobs) {
        if (config.getAbsoluteMaxSizeMB() < 0 || getStorageSize() < config.getAbsoluteMaxSizeMB() * (1024 * 1024)) {
            // Store blobs on the file system
            List<Map<String, String>> blobInfos = storeBlobs(key, blobs);
            // Persist blob information in the store
            persistBlobs(key, getSizeOfBlobs(blobs), blobInfos);
        } else {
            throw new MaximumTransientSpaceExceeded();
        }
    }

    protected List<Map<String, String>> storeBlobs(String key, List<Blob> blobs) {
        if (blobs == null) {
            return null;
        }
        // Store blobs on the file system and compute blob information
        List<Map<String, String>> blobInfos = new ArrayList<>();
        for (Blob blob : blobs) {
            Map<String, String> blobInfo = new HashMap<>();
            File cachingDir = getCachingDirectory(key);
            String uuid = UUID.randomUUID().toString();
            File cachedFile = new File(cachingDir, uuid);
            try {
                if (blob instanceof FileBlob && ((FileBlob) blob).isTemporary()) {
                    ((FileBlob) blob).moveTo(cachedFile);
                } else {
                    blob.transferTo(cachedFile);
                }
            } catch (IOException e) {
                throw new NuxeoException(e);
            }
            Path cachedFileRelativePath = Paths.get(cachingDir.getName(), uuid);
            blobInfo.put("file", cachedFileRelativePath.toString());
            // Redis doesn't support null values
            if (blob.getFilename() != null) {
                blobInfo.put("filename", blob.getFilename());
            }
            if (blob.getEncoding() != null) {
                blobInfo.put("encoding", blob.getEncoding());
            }
            if (blob.getMimeType() != null) {
                blobInfo.put("mimetype", blob.getMimeType());
            }
            if (blob.getDigest() != null) {
                blobInfo.put("digest", blob.getDigest());
            }
            blobInfos.add(blobInfo);
        }
        log.debug("Stored blobs on the file system: " + blobInfos);
        return blobInfos;
    }

    public File getCachingDirectory(String key) {
        String cachingDirName = getCachingDirName(key);
        try {
            File cachingDir = new File(cacheDir.getCanonicalFile(), cachingDirName);
            if (!cachingDir.getCanonicalPath().startsWith(cacheDir.getCanonicalPath())) {
                throw new NuxeoException("Trying to traverse illegal path: " + cachingDir + " for key: " + key);
            }
            if (!cachingDir.exists()) {
                cachingDir.mkdir();
            }
            return cachingDir;
        } catch (IOException e) {
            throw new NuxeoException("Error when trying to access cache directory: " + cacheDir + "/" + cachingDirName
                    + " for key: " + key, e);
        }
    }

    protected String getCachingDirName(String key) {
        String dirName = Base64.encodeBase64String(key.getBytes());
        dirName = dirName.replaceAll("/", "_");
        return dirName;
    }

    protected long getSizeOfBlobs(List<Blob> blobs) {
        int size = 0;
        if (blobs != null) {
            for (Blob blob : blobs) {
                long blobLength = blob.getLength();
                if (blobLength > -1) {
                    size += blobLength;
                }
            }
        }
        return size;
    }

    protected List<Blob> loadBlobs(List<Map<String, String>> blobInfos) {
        log.debug("Loading blobs from the file system: " + blobInfos);
        List<Blob> blobs = new ArrayList<>();
        for (Map<String, String> info : blobInfos) {
            File blobFile = new File(cacheDir, info.get("file"));
            Blob blob = new FileBlob(blobFile);
            blob.setEncoding(info.get("encoding"));
            blob.setMimeType(info.get("mimetype"));
            blob.setFilename(info.get("filename"));
            blob.setDigest(info.get("digest"));
            blobs.add(blob);
        }
        return blobs;
    }

    @Override
    public void doGC() {
        log.debug(String.format("Performing GC for TransientStore %s", config.getName()));
        long newSize = 0;
        try {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(cacheDir.getAbsolutePath()))) {
                for (Path entry : stream) {
                    String key = getKeyCachingDirName(entry.getFileName().toString());
                    if (exists(key)) {
                        newSize += getFilePathSize(entry);
                        continue;
                    }
                    FileUtils.deleteQuietly(entry.toFile());
                }
            }
        } catch (IOException e) {
            log.error("Error while performing GC", e);
        }
        setStorageSize(newSize);
    }

    protected String getKeyCachingDirName(String dir) {
        String key = dir.replaceAll("_", "/");
        return new String(Base64.decodeBase64(key));
    }

    protected long getFilePathSize(Path entry) {
        long size = 0;
        for (File file : entry.toFile().listFiles()) {
            size += file.length();
        }
        return size;
    }

    @Override
    public void removeAll() {
        log.debug("Removing all entries from TransientStore " + config.getName());
        removeAllEntries();
        doGC();
    }

    public File getCacheDir() {
        return cacheDir;
    }

}
