/*
 * (C) Copyright 2008-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    protected BinaryManagerDescriptor descriptor;

    public void initialize(RepositoryDescriptor repositoryDescriptor)
            throws IOException {
        String path = repositoryDescriptor.binaryStorePath;
        if (path == null || path.trim().length() == 0) {
            path = DEFAULT_PATH;
        }
        path = path.trim();
        File base;
        if (path.startsWith("/") || path.startsWith("\\")
                || path.contains("://") || path.contains(":\\")) {
            // absolute
            base = new File(path);
        } else {
            // relative
            String home = Framework.getRuntime().getHome().getPath();
            if (home.endsWith("/") || home.endsWith("\\")) {
                home = home.substring(0, home.length() - 1);
            }
            base = new File(home, path);
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

    public Binary getBinary(InputStream in) throws IOException {
        /*
         * First, write the input stream to a temporary file, while computing a
         * digest.
         */
        File tmp = File.createTempFile("create_", ".tmp", tmpDir);
        tmp.deleteOnExit();
        String digest;
        OutputStream out = new FileOutputStream(tmp);
        try {
            digest = storeAndDigest(in, out);
        } finally {
            out.close();
        }
        /*
         * Move the tmp file to its destination.
         */
        File file = getFileForDigest(digest, true);
        tmp.renameTo(file); // atomic move, fails if already there
        tmp.delete(); // fails if the move was successful
        if (!file.exists()) {
            throw new IOException("Could not create file: " + file);
        }
        /*
         * Now we can build the Binary.
         */
        return getBinaryScrambler().getUnscrambledBinary(file, digest);
    }

    public Binary getBinary(String digest) {
        File file = getFileForDigest(digest, false);
        if (file == null || !file.exists()) {
            return null;
        }
        return getBinaryScrambler().getUnscrambledBinary(file, digest);
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

    /**
     * A {@link BinaryScrambler} that does nothing.
     */
    public static class NullBinaryScrambler implements BinaryScrambler {
        public static final BinaryScrambler INSTANCE = new NullBinaryScrambler();

        public void scrambleBuffer(byte[] buf, int off, int n) {
        }

        public void unscrambleBuffer(byte[] buf, int off, int n) {
        }

        public Binary getUnscrambledBinary(File file, String digest) {
            return new Binary(file, digest);
        }

        public void skip(long n) {
        }

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

        public ScrambledBinary(File file, String digest,
                BinaryScrambler scrambler) {
            super(file, digest);
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

        protected final byte onebyte[] = new byte[1];

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
        public int read(byte b[]) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte b[], int off, int len) throws IOException {
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
}
