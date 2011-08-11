/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume, jcarsique
 */

package org.nuxeo.ecm.core.storage.sql;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.streaming.FileSource;
import org.nuxeo.runtime.services.streaming.StreamSource;

/**
 * A simple filesystem-based binary manager. It stores the binaries according to
 * their digest (hash), which means that no transactional behavior needs to be
 * implemented.
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
 */
public class DefaultBinaryManager implements BinaryManager {

    private static final Log log = LogFactory.getLog(DefaultBinaryManager.class);

    public static final String DEFAULT_DIGEST = "MD5"; // "SHA-256"

    public static final int DEFAULT_DEPTH = 2;

    public static final String DEFAULT_PATH = "binaries";

    public static final String DATA = "data";

    public static final String TMP = "tmp";

    public static final String CONFIG_FILE = "config.xml";

    protected File storageDir;

    protected File tmpDir;

    protected String repositoryName;

    protected BinaryManagerDescriptor descriptor;

    protected BinaryGarbageCollector garbageCollector;

    @Override
    public void initialize(RepositoryDescriptor repositoryDescriptor)
            throws IOException {
        String path = repositoryDescriptor.binaryStorePath;
        if (path == null || path.trim().length() == 0) {
            path = DEFAULT_PATH;
        }
        path = Framework.expandVars(path);
        path = path.trim();
        File base;
        if (path.startsWith("/") || path.startsWith("\\")
                || path.contains("://") || path.contains(":\\")) {
            // absolute
            base = new File(path);
        } else {
            // relative
            File home = Environment.getDefault().getData();
            base = new File(home, path);

            // Backward compliance with versions before 5.4 (NXP-5370)
            File oldBase = new File(Framework.getRuntime().getHome().getPath(),
                    path);
            if (oldBase.exists()) {
                log.warn("Old binaries path used (NXP-5370). Please move "
                        + oldBase + " to " + base);
                base = oldBase;
            }
        }

        log.info("Repository '"
                + repositoryDescriptor.name
                + "' using "
                + (this.getClass().equals(DefaultBinaryManager.class) ? ""
                        : (this.getClass().getSimpleName() + " and "))
                + "binary store: " + base);
        storageDir = new File(base, DATA);
        tmpDir = new File(base, TMP);
        storageDir.mkdirs();
        tmpDir.mkdirs();
        descriptor = getDescriptor(new File(base, CONFIG_FILE));
        createGarbageCollector();
    }

    public File getStorageDir() {
        return storageDir;
    }

    /**
     * Gets existing descriptor or creates a default one.
     */
    protected BinaryManagerDescriptor getDescriptor(File configFile)
            throws IOException {
        BinaryManagerDescriptor desc;
        if (configFile.exists()) {
            XMap xmap = new XMap();
            xmap.register(BinaryManagerDescriptor.class);
            try {
                desc = (BinaryManagerDescriptor) xmap.load(new FileInputStream(
                        configFile));
            } catch (Exception e) {
                throw (IOException) new IOException().initCause(e);
            }
        } else {
            desc = new BinaryManagerDescriptor();
            // TODO fetch from repo descriptor
            desc.digest = DEFAULT_DIGEST;
            desc.depth = DEFAULT_DEPTH;
            desc.write(configFile); // may throw IOException
        }
        return desc;
    }

    protected BinaryScrambler getBinaryScrambler() {
        return NullBinaryScrambler.INSTANCE;
    }

    @Override
    public Binary getBinary(InputStream in) throws IOException {
        /*
         * First, write the input stream to a temporary file, while computing a
         * digest.
         */
        File tmp = File.createTempFile("create_", ".tmp", tmpDir);
        try {
            String digest;
            OutputStream out = new FileOutputStream(tmp);
            try {
                digest = storeAndDigest(in, out);
            } finally {
                in.close();
                out.close();
            }
            /*
             * Move the tmp file to its destination.
             */
            File file = getFileForDigest(digest, true);
            tmp.renameTo(file); // atomic move, fails if already there
            if (!file.exists()) {
                throw new IOException("Could not create file: " + file);
            }
            /*
             * Now we can build the Binary.
             */
            return getBinaryScrambler().getUnscrambledBinary(file, digest,
                    repositoryName);
        } finally {
            tmp.delete(); // fails if the move was successful
        }
    }

    @Override
    public Binary getBinary(String digest) {
        File file = getFileForDigest(digest, false);
        if (file == null || !file.exists()) {
            log.warn("cannot fetch content at " + file.getPath()
                    + " (file does not exist), check your configuration");
            return null;
        }
        return getBinaryScrambler().getUnscrambledBinary(file, digest,
                repositoryName);
    }

    /**
     * Gets a file representing the storage for a given digest.
     *
     * @param digest the digest
     * @param createDir {@code true} if the directory containing the file itself
     *            must be created
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

    public static final int MIN_BUF_SIZE = 8 * 1024; // 8 kB

    public static final int MAX_BUF_SIZE = 64 * 1024; // 64 kB

    protected String storeAndDigest(InputStream in, OutputStream out)
            throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(descriptor.digest);
        } catch (NoSuchAlgorithmException e) {
            throw (IOException) new IOException().initCause(e);
        }

        int size = in.available();
        if (size == 0) {
            size = MAX_BUF_SIZE;
        } else if (size < MIN_BUF_SIZE) {
            size = MIN_BUF_SIZE;
        } else if (size > MAX_BUF_SIZE) {
            size = MAX_BUF_SIZE;
        }
        byte[] buf = new byte[size];

        /*
         * Scramble, copy and digest.
         */
        BinaryScrambler scrambler = getBinaryScrambler();
        int n;
        while ((n = in.read(buf)) != -1) {
            scrambler.scrambleBuffer(buf, 0, n);
            digest.update(buf, 0, n);
            out.write(buf, 0, n);
        }
        out.flush();

        return toHexString(digest.digest());
    }

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    public static String toHexString(byte[] data) {
        StringBuilder buf = new StringBuilder(2 * data.length);
        for (byte b : data) {
            buf.append(HEX_DIGITS[(0xF0 & b) >> 4]);
            buf.append(HEX_DIGITS[0x0F & b]);
        }
        return buf.toString();
    }

    protected void createGarbageCollector() {
        garbageCollector = new DefaultBinaryGarbageCollector(this);
    }

    @Override
    public BinaryGarbageCollector getGarbageCollector() {
        return garbageCollector;
    }

    /**
     * A {@link BinaryScrambler} that does nothing.
     */
    public static class NullBinaryScrambler implements BinaryScrambler {
        public static final BinaryScrambler INSTANCE = new NullBinaryScrambler();

        @Override
        public void scrambleBuffer(byte[] buf, int off, int n) {
        }

        @Override
        public void unscrambleBuffer(byte[] buf, int off, int n) {
        }

        @Override
        public Binary getUnscrambledBinary(File file, String digest,
                String repoName) {
            return new Binary(file, digest, repoName);
        }

        @Override
        public void skip(long n) {
        }

        @Override
        public void reset() {
        }
    }

    /**
     * A {@link Binary} that is unscrambled on read using a
     * {@link BinaryScrambler}.
     */
    public static class ScrambledBinary extends Binary {

        private static final long serialVersionUID = 1L;

        private final File file;

        protected final BinaryScrambler scrambler;

        public ScrambledBinary(File file, String digest, String repoName,
                BinaryScrambler scrambler) {
            super(file, digest, repoName);
            this.file = file;
            this.scrambler = scrambler;
        }

        @Override
        public InputStream getStream() throws IOException {
            return new ScrambledFileInputStream(file, scrambler);
        }

        @Override
        public StreamSource getStreamSource() {
            return new ScrambledStreamSource(file, scrambler);
        }

    }

    /**
     * A {@link FileSource} that is unscrambled on read using a
     * {@link BinaryScrambler}.
     */
    public static class ScrambledStreamSource extends FileSource {

        protected final BinaryScrambler scrambler;

        public ScrambledStreamSource(File file, BinaryScrambler scrambler) {
            super(file);
            this.scrambler = scrambler;
        }

        @Override
        public File getFile() {
            throw new UnsupportedOperationException();
        }

        @Override
        public InputStream getStream() throws IOException {
            return new ScrambledFileInputStream(file, scrambler);
        }
    }

    /**
     * A {@link FileInputStream} that is unscrambled on read using a
     * {@link BinaryScrambler}.
     */
    public static class ScrambledFileInputStream extends InputStream {

        protected final InputStream is;

        protected final BinaryScrambler scrambler;

        protected final byte[] onebyte = new byte[1];

        protected ScrambledFileInputStream(File file, BinaryScrambler scrambler)
                throws IOException {
            is = new FileInputStream(file);
            this.scrambler = scrambler;
            scrambler.reset();
        }

        @Override
        public int read() throws IOException {
            int b = is.read();
            if (b != -1) {
                onebyte[0] = (byte) b;
                scrambler.unscrambleBuffer(onebyte, 0, 1);
                b = onebyte[0];
            }
            return b;
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = is.read(b, off, len);
            if (n != -1) {
                scrambler.unscrambleBuffer(b, off, n);
            }
            return n;
        }

        @Override
        public long skip(long n) throws IOException {
            n = is.skip(n);
            scrambler.skip(n);
            return n;
        }

        @Override
        public int available() throws IOException {
            return is.available();
        }

        @Override
        public void close() throws IOException {
            is.close();
        }
    }

    public static class DefaultBinaryGarbageCollector implements
            BinaryGarbageCollector {

        /**
         * Windows FAT filesystems have a time resolution of 2s. Other common
         * filesystems have 1s.
         */
        public static int TIME_RESOLUTION = 2000;

        protected final DefaultBinaryManager binaryManager;

        protected volatile long startTime;

        protected BinaryManagerStatus status;

        public DefaultBinaryGarbageCollector(DefaultBinaryManager binaryManager) {
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
                throw new RuntimeException("Alread started");
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
            deleteOld(binaryManager.getStorageDir(), startTime
                    - TIME_RESOLUTION, 0, delete);
            status.gcDuration = System.currentTimeMillis() - startTime;
            startTime = 0;
        }

        protected void deleteOld(File file, long minTime, int depth,
                boolean delete) {
            if (file.isDirectory()) {
                for (File f : file.listFiles()) {
                    deleteOld(f, minTime, depth + 1, delete);
                }
                if (depth > 0 && file.list().length == 0) {
                    // empty directory
                    file.delete();
                }
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
            RandomAccessFile r = new RandomAccessFile(file, "rw");
            try {
                r.setLength(r.length());
            } finally {
                r.close();
            }
        } catch (IOException e) {
            log.error("Cannot set last modified for file: " + file, e);
        }
    }

}
