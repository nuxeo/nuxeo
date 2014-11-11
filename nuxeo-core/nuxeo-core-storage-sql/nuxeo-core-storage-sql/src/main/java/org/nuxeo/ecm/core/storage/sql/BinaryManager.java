/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import org.nuxeo.common.xmap.XMap;
import org.nuxeo.runtime.api.Framework;

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
public class BinaryManager {

    public static final String DEFAULT_DIGEST = "MD5"; // "SHA-256"

    public static final int DEFAULT_DEPTH = 2;

    public static final String BINARIES = "binaries";

    public static final String DATA = "data";

    public static final String TMP = "tmp";

    public static final String CONFIG_FILE = "config.xml";

    private final File storageDir;

    private final File tmpDir;

    private final String digestAlgorithm;

    private final int depth;

    public BinaryManager(RepositoryDescriptor descriptor) throws IOException {
        File base = new File(Framework.getRuntime().getHome(), BINARIES);
        storageDir = new File(base, DATA);
        tmpDir = new File(base, TMP);
        storageDir.mkdirs();
        tmpDir.mkdirs();
        BinaryManagerDescriptor desc;
        File configFile = new File(base, CONFIG_FILE);
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
            desc.write(configFile);
        }
        digestAlgorithm = desc.digest;
        depth = desc.depth;
    }

    /**
     * Returns a {@link Binary} representing a given input stream.
     * <p>
     * The input stream is read, and a filesystem representation created
     * accordingly.
     *
     * @param in the input stream
     * @return the corresponding binary
     * @throws IOException
     */
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
        return new Binary(file, digest);
    }

    /**
     * Returns a {@link Binary} corresponding to the given digest.
     * <p>
     * A {@code null} will be returned if the digest could not be found in the
     * filesystem, which is a system administration error.
     *
     * @param digest the digest
     * @return the corresponding binary
     */

    public Binary getBinary(String digest) {
        File file = getFileForDigest(digest, false);
        if (!file.exists()) {
            return null;
        }
        return new Binary(file, digest);
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
            digest = MessageDigest.getInstance(digestAlgorithm);
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
         * Copy and digest.
         */
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
            digest.update(buf, 0, n);
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

}
