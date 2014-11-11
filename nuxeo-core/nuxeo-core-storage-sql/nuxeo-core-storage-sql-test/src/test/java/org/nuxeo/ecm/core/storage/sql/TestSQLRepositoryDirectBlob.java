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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

import org.nuxeo.common.Environment;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.storage.StorageBlob;
import org.nuxeo.ecm.core.storage.binary.Binary;
import org.nuxeo.ecm.core.storage.binary.BinaryManager;
import org.nuxeo.ecm.core.storage.binary.BinaryManagerDescriptor;
import org.nuxeo.ecm.core.storage.binary.BinaryManagerService;
import org.nuxeo.ecm.core.storage.binary.DefaultBinaryManager;
import org.nuxeo.ecm.core.storage.binary.LocalBinaryManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.streaming.FileSource;

/**
 * Sample test showing how to use a direct access to the binaries storage.
 *
 * @author Florent Guillaume
 */
public class TestSQLRepositoryDirectBlob extends SQLRepositoryTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.core.storage.sql.test.tests",
                "OSGI-INF/test-repo-core-types-contrib.xml");
        openSession();
    }

    @Override
    @After
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
    @Test
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
        BinaryManager binaryManager = new DefaultBinaryManager();
        binaryManager.initialize(new BinaryManagerDescriptor());
        Binary binary = binaryManager.getBinary(digest);
        if (binary == null) {
            throw new RuntimeException("Missing file for digest: " + digest);
        }
        String filename = "doc.txt";
        Blob blob = new StorageBlob(binary, filename, "text/plain", "utf-8",
                binary.getDigest(), binary.getLength());
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

    @Test
    public void testBinarySerialization() throws Exception {
        DocumentModel folder = session.getRootDocument();
        DocumentModel file = new DocumentModelImpl(folder.getPathAsString(),
                "filea", "File");
        file = session.createDocument(file);
        session.save();

        // create a binary instance pointing to some content stored on the
        // filesystem
        String digest = createFile();
        BinaryManager binaryManager = new DefaultBinaryManager();
        binaryManager.initialize(new BinaryManagerDescriptor());
        Binary binary = binaryManager.getBinary(digest);
        if (binary == null) {
            throw new RuntimeException("Missing file for digest: " + digest);
        }

        String expected = "this is a file";
        byte[] observedContent = new byte[expected.length()];
        assertEquals(digest, binary.getDigest());
        assertEquals(expected.length(), binary.getLength());
        assertEquals(expected.length(),
                binary.getStream().read(observedContent));
        assertEquals(expected, new String(observedContent));

        // serialize and deserialize the binary instance
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(binary);
        out.flush();
        out.close();

        // Make an input stream from the byte array and read
        // a copy of the object back in.
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(
                bos.toByteArray()));
        Binary binaryCopy = (Binary) in.readObject();

        observedContent = new byte[expected.length()];
        assertEquals(digest, binaryCopy.getDigest());
        assertEquals(expected.length(), binaryCopy.getLength());
        assertEquals(expected.length(), binaryCopy.getStream().read(
                observedContent));
        assertEquals(expected, new String(observedContent));
    }

    protected static class TmpStreamingBlob extends StreamingBlob {
        private static final long serialVersionUID = 1L;

        public TmpStreamingBlob(FileSource src) {
            super(src);
        }

        @Override
        public boolean isTemporary() {
            return true;
        }
    }

    @Test
    public void testBinaryManagerTmpFileMoveNotCopy() throws Exception {
        BinaryManagerService bms = Framework.getLocalService(BinaryManagerService.class);
        LocalBinaryManager binaryManager = (LocalBinaryManager) bms.getBinaryManager(session.getRepositoryName());

        // tmp file in binary manager filesystem (not in tmp but still works)
        File file = File.createTempFile("test-", ".data",
                binaryManager.getStorageDir());
        FileOutputStream out = new FileOutputStream(file);
        IOUtils.copy(new ByteArrayInputStream("abcd\n".getBytes("UTF-8")), out);
        out.close();

        // create blob
        FileSource fileSource = new FileSource(file);
        Blob blob = new TmpStreamingBlob(fileSource);

        // set in doc
        DocumentModel doc = new DocumentModelImpl("/", "myfile", "File");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);
        session.save();

        assertFalse(file.exists());
        assertTrue(fileSource.getFile().exists());
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
        File rootDir = new File(Environment.getDefault().getData(), "binaries");
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
