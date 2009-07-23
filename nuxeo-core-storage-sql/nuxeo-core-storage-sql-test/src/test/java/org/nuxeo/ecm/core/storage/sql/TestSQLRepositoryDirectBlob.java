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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLBlob;
import org.nuxeo.runtime.api.Framework;

/**
 * Sample test showing how to use a direct access to the binaries storage.
 *
 * @author Florent Guillaume
 */
public class TestSQLRepositoryDirectBlob extends SQLRepositoryTestCase {

    public TestSQLRepositoryDirectBlob(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.core.storage.sql.test.tests",
                "OSGI-INF/test-repo-core-types-contrib.xml");
        openSession();
    }

    @Override
    public void tearDown() throws Exception {
        session.cancel();
        closeSession();
        super.tearDown();
    }

    // ----- Third-party application -----

    /** The application that creates a file. */
    public String createFile() throws Exception {
        FileManager fileMaker = new FileManager();

        // get the tmp dir where to create files
        File tmpDir = fileMaker.getTmpDir();

        // third-party application creates a file there
        File file = File.createTempFile("myapp", null, tmpDir);
        FileOutputStream out = new FileOutputStream(file);
        out.write("this is a file".getBytes("UTF-8"));
        out.close();

        // then it moves the tmp file to the binaries storage, and gets the
        // digest
        String digest = fileMaker.moveTmpFileToBinaries(file);
        return digest;
    }

    // ----- Nuxeo application -----

    public void testDirectBlob() throws Exception {
        DocumentModel folder = session.getRootDocument();
        DocumentModel file = new DocumentModelImpl(folder.getPathAsString(),
                "filea", "File");
        file = session.createDocument(file);
        session.save();

        /*
         * 1. A third-party application returns a digest for a created file.
         */
        String digest = createFile();

        /*
         * 2. Later, create and use the blob for this digest.
         */
        BinaryManager binaryManager = new BinaryManager(null);
        Binary binary = binaryManager.getBinary(digest);
        if (binary == null) {
            throw new RuntimeException("Missing file for digest: " + digest);
        }
        String filename = "doc.txt";
        Blob blob = new SQLBlob(binary, filename, "text/plain", "utf-8",
                binary.getDigest());
        file.setProperty("file", "filename", filename);
        file.setProperty("file", "content", blob);
        session.saveDocument(file);
        session.save();

        /*
         * 3. Check the retrieved doc.
         */
        String expected = "this is a file";
        file = session.getDocument(file.getRef());
        blob = (Blob) file.getProperty("file", "content");
        assertEquals("doc.txt", blob.getFilename());
        assertEquals(expected.length(), blob.getLength());
        assertEquals("utf-8", blob.getEncoding());
        assertEquals("text/plain", blob.getMimeType());
        assertEquals(expected, blob.getString());

        /*
         * remove attached file
         */
       file.setProperty("file", "content", null);
       file = session.saveDocument(file);
       session.save();
       assertNull(file.getProperty("file", "content"));

    }

    public void testBinarySerialization() throws Exception {
        DocumentModel folder = session.getRootDocument();
        DocumentModel file = new DocumentModelImpl(folder.getPathAsString(),
                "filea", "File");
        file = session.createDocument(file);
        session.save();

        // create a binary instance pointing to some content stored on the
        // filesystem
        String digest = createFile();
        BinaryManager binaryManager = new BinaryManager(null);
        Binary binary = binaryManager.getBinary(digest);
        if (binary == null) {
            throw new RuntimeException("Missing file for digest: " + digest);
        }

        String expected = "this is a file";
        byte[] observedContent = new byte[expected.length()];
        assertEquals(digest, binary.getDigest());
        assertEquals(expected.length(), binary.getLength());
        assertEquals(expected.length(), binary.getStream().read(observedContent));
        assertEquals(expected, new String(observedContent));

        // serialize and deserialize the binary instance
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(binary);
        out.flush();
        out.close();

        // Make an input stream from the byte array and read
        // a copy of the object back in.
        ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bos.toByteArray()));
        Binary binaryCopy = (Binary) in.readObject();

        observedContent = new byte[expected.length()];
        assertEquals(digest, binaryCopy.getDigest());
        assertEquals(expected.length(), binaryCopy.getLength());
        assertEquals(expected.length(), binaryCopy.getStream().read(observedContent));
        assertEquals(expected, new String(observedContent));
    }
}

/**
 * Class doing a simplified version of what the binaries storage does.
 * <p>
 * In a real application, change the constructor to pass the rootDir as a
 * parameter or use configuration.
 *
 * @author Florent Guillaume
 */
class FileManager {

    /*
     * These parameters have to be the same as the one from the binaries
     * storage.
     */

    public static final String DIGEST_ALGORITHM = "MD5";

    public static final int DEPTH = 2;

    protected final File tmpDir;

    protected final File dataDir;

    public FileManager() {
        // from inside Nuxeo components, this can be used
        // otherwise use a hardcoded string or parameter to that directory
        File rootDir = new File(Framework.getRuntime().getHome(), "binaries");
        tmpDir = new File(rootDir, "tmp");
        dataDir = new File(rootDir, "data");
        tmpDir.mkdirs();
        dataDir.mkdirs();
    }

    public File getTmpDir() {
        return tmpDir;
    }

    public String moveTmpFileToBinaries(File file) throws IOException {
        // digest the file
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance(DIGEST_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw (IOException) new IOException().initCause(e);
        }
        FileInputStream in = new FileInputStream(file);
        try {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                messageDigest.update(buf, 0, n);
            }
        } finally {
            in.close();
        }
        String digest = toHexString(messageDigest.digest());

        // move the file to its final location
        File dest = getFileForDigest(digest, dataDir);
        file.renameTo(dest); // atomic move, fails if already there
        file.delete(); // fails if the move was successful
        if (!dest.exists()) {
            throw new IOException("Could not create file: " + dest);
        }
        return digest;
    }

    protected static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    protected static String toHexString(byte[] data) {
        StringBuilder buf = new StringBuilder(2 * data.length);
        for (byte b : data) {
            buf.append(HEX_DIGITS[(0xF0 & b) >> 4]);
            buf.append(HEX_DIGITS[0x0F & b]);
        }
        return buf.toString();
    }

    protected static File getFileForDigest(String digest, File dataDir) {
        StringBuilder buf = new StringBuilder(3 * DEPTH - 1);
        for (int i = 0; i < DEPTH; i++) {
            if (i != 0) {
                buf.append(File.separatorChar);
            }
            buf.append(digest.substring(2 * i, 2 * i + 2));
        }
        File dir = new File(dataDir, buf.toString());
        dir.mkdirs();
        return new File(dir, digest);
    }

}
