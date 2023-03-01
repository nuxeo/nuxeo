/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 *     Mathieu Guillaume
 *     jcarsique
 */

package org.nuxeo.ecm.core.blob.binary;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.LocalBlobStoreConfiguration;
import org.nuxeo.runtime.trackers.files.FileEventTracker;

/**
 * A simple filesystem-based binary manager. It stores the binaries according to their digest (hash), which means that
 * no transactional behavior needs to be implemented.
 * <p>
 * A garbage collection is needed to purge unused binaries.
 * <p>
 * The format of the <em>binaries</em> directory is:
 * <ul>
 * <li><em>data/</em> hierarchy with the actual binaries in subdirectories,</li>
 * <li><em>tmp/</em> temporary storage during creation,</li>
 * <li><em>config.xml</em> a file containing the configuration used.</li>
 * </ul>
 *
 * @author Florent Guillaume
 * @since 5.6
 */
public class LocalBinaryManager extends AbstractBinaryManager {

    private static final Log log = LogFactory.getLog(LocalBinaryManager.class);

    /** @deprecated since 11.1, use {@link LocalBlobStoreConfiguration} instead */
    @Deprecated
    public static final Pattern WINDOWS_ABSOLUTE_PATH = Pattern.compile("[a-zA-Z]:[/\\\\].*");

    /** @deprecated since 11.1, use {@link LocalBlobStoreConfiguration} instead */
    @Deprecated
    public static final String DEFAULT_PATH = "binaries";

    /** @deprecated since 11.1, use {@link LocalBlobStoreConfiguration} instead */
    @Deprecated
    public static final String DATA = "data";

    /** @deprecated since 11.1, use {@link LocalBlobStoreConfiguration} instead */
    @Deprecated
    public static final String TMP = "tmp";

    /** @deprecated since 11.1, use {@link LocalBlobStoreConfiguration} instead */
    @Deprecated
    public static final String CONFIG_FILE = "config.xml";

    protected File storageDir;

    protected File tmpDir;

    @Override
    public void initialize(String blobProviderId, Map<String, String> properties) throws IOException {
        super.initialize(blobProviderId, properties);
        LocalBlobStoreConfiguration config = new LocalBlobStoreConfiguration(properties);

        log.info("Registering binary manager '" + blobProviderId + "' using "
                + (this.getClass().equals(LocalBinaryManager.class) ? "" : (this.getClass().getSimpleName() + " and "))
                + "binary store: " + config.storageDir.getParent());
        storageDir = config.storageDir.toFile();
        tmpDir = config.tmpDir.toFile();
        storageDir.mkdirs();
        tmpDir.mkdirs();
        setDescriptor(config.descriptor);
        createGarbageCollector();

        // be sure FileTracker won't steal our files !
        FileEventTracker.registerProtectedPath(storageDir.getAbsolutePath());
    }

    @Override
    public void close() {
        if (tmpDir != null) {
            try {
                FileUtils.cleanDirectory(tmpDir);
            } catch (IOException e) {
                throw new NuxeoException(e);
            }
        }
    }

    public File getStorageDir() {
        return storageDir;
    }

    @Override
    protected Binary getBinary(InputStream in) throws IOException {
        String digest = storeAndDigest(in);
        File file = getFileForDigest(digest, false);
        /*
         * Now we can build the Binary.
         */
        return new Binary(file, digest, blobProviderId);
    }

    @Override
    public Binary getBinary(String digest) {
        File file = getFileForDigest(digest, false);
        if (file == null) {
            // invalid digest
            return null;
        }
        if (!file.exists()) {
            log.warn("cannot fetch content at " + file.getPath() + " (file does not exist), check your configuration");
            return null;
        }
        return new Binary(file, digest, blobProviderId);
    }

    /**
     * Gets a file representing the storage for a given digest.
     *
     * @param digest the digest
     * @param createDir {@code true} if the directory containing the file itself must be created
     * @return the file for this digest
     */
    public File getFileForDigest(String digest, boolean createDir) {
        int depth = descriptor.depth;
        if (digest.length() < 2 * depth) {
            return null;
        }
        StringBuilder buf = new StringBuilder(3 * depth - 1);
        for (int i = 0; i < depth; i++) {
            if (i != 0) {
                buf.append(File.separatorChar);
            }
            buf.append(digest.substring(2 * i, 2 * i + 2));
        }
        File dir = new File(storageDir, buf.toString());
        if (createDir) {
            dir.mkdirs();
        }
        return new File(dir, digest);
    }

    protected String storeAndDigest(InputStream in) throws IOException {
        File tmp = File.createTempFile("create_", ".tmp", tmpDir);
        /*
         * First, write the input stream to a temporary file, while computing a digest.
         */
        try {
            String digest;
            try (OutputStream out = new FileOutputStream(tmp)) {
                digest = storeAndDigest(in, out);
            } finally {
                in.close();
            }
            /*
             * Move the tmp file to its destination.
             */
            File file = getFileForDigest(digest, true);
            atomicMove(tmp, file);
            return digest;
        } finally {
            tmp.delete();
        }

    }

    /**
     * Does an atomic move of the tmp (or source) file to the final file.
     * <p>
     * Tries to work well with NFS mounts and different filesystems.
     */
    protected void atomicMove(File source, File dest) throws IOException {
        if (dest.exists()) {
            // The file with the proper digest is already there so don't do
            // anything. This is to avoid "Stale NFS File Handle" problems
            // which would occur if we tried to overwrite it anyway.
            // Note that this doesn't try to protect from the case where
            // two identical files are uploaded at the same time.
            // Update date for the GC.
            dest.setLastModified(source.lastModified());
            return;
        }
        if (!source.renameTo(dest)) {
            // Failed to rename, probably a different filesystem.
            // Do *NOT* use Apache Commons IO's FileUtils.moveFile()
            // because it rewrites the destination file so is not atomic.
            // Do a copy through a tmp file on the same filesystem then
            // atomic rename.
            File tmp = File.createTempFile(dest.getName(), ".tmp", dest.getParentFile());
            try {
                try (InputStream in = new FileInputStream(source); //
                        OutputStream out = new FileOutputStream(tmp)) {
                    IOUtils.copy(in, out);
                }
                // then do the atomic rename
                tmp.renameTo(dest);
            } finally {
                tmp.delete();
            }
            // finally remove the original source
            source.delete();
        }
        if (!dest.exists()) {
            throw new IOException("Could not create file: " + dest);
        }
    }

    protected void createGarbageCollector() {
        garbageCollector = new DefaultBinaryGarbageCollector(this);
    }

    public static class DefaultBinaryGarbageCollector implements BinaryGarbageCollector {

        /**
         * Windows FAT filesystems have a time resolution of 2s. Other common filesystems have 1s.
         */
        public static final int TIME_RESOLUTION = 2000;

        protected final LocalBinaryManager binaryManager;

        protected volatile long startTime;

        protected BinaryManagerStatus status;

        public DefaultBinaryGarbageCollector(LocalBinaryManager binaryManager) {
            this.binaryManager = binaryManager;
        }

        @Override
        public String getId() {
            return binaryManager.getStorageDir().toURI().toString();
        }

        @Override
        public BinaryManagerStatus getStatus() {
            return status;
        }

        @Override
        public boolean isInProgress() {
            // volatile as this is designed to be called from another thread
            return startTime != 0;
        }

        @Override
        public void start() {
            if (startTime != 0) {
                throw new RuntimeException("Already started");
            }
            startTime = System.currentTimeMillis();
            status = new BinaryManagerStatus();
        }

        @Override
        public void mark(String digest) {
            File file = binaryManager.getFileForDigest(digest, false);
            if (!file.exists()) {
                log.error("Unknown file digest: " + digest);
                return;
            }
            touch(file);
        }

        @Override
        public void stop(boolean delete) {
            if (startTime == 0) {
                throw new RuntimeException("Not started");
            }
            deleteOld(binaryManager.getStorageDir(), startTime - TIME_RESOLUTION, 0, delete);
            status.gcDuration = System.currentTimeMillis() - startTime;
            startTime = 0;
        }

        @Override
        public void reset() {
            startTime = 0;
        }

        protected void deleteOld(File file, long minTime, int depth, boolean delete) {
            if (file.isDirectory()) {
                for (File f : file.listFiles()) {
                    deleteOld(f, minTime, depth + 1, delete);
                }
                // NXP-30465: do not delete empty directories
            } else if (file.isFile() && file.canWrite()) {
                long lastModified = file.lastModified();
                long length = file.length();
                if (lastModified == 0) {
                    log.error("Cannot read last modified for file: " + file);
                } else if (lastModified < minTime) {
                    status.sizeBinariesGC += length;
                    status.numBinariesGC++;
                    if (delete && !file.delete()) {
                        log.warn("Cannot gc file: " + file);
                    }
                } else {
                    status.sizeBinaries += length;
                    status.numBinaries++;
                }
            }
        }
    }

    /**
     * Sets the last modification date to now on a file
     *
     * @param file the file
     */
    public static void touch(File file) {
        long time = System.currentTimeMillis();
        if (file.setLastModified(time)) {
            // ok
            return;
        }
        if (!file.canWrite()) {
            // cannot write -> stop won't be able to delete anyway
            return;
        }
        try {
            // Windows: the file may be open for reading
            // workaround found by Thomas Mueller, see JCR-2872
            try (RandomAccessFile r = new RandomAccessFile(file, "rw")) {
                r.setLength(r.length());
            }
        } catch (IOException e) {
            log.error("Cannot set last modified for file: " + file, e);
        }
    }

}
