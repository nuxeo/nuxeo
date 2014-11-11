/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.blob.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

import junit.framework.TestCase;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.blob.storage.impl.DefaultBlobStorage;
import org.nuxeo.ecm.core.blob.storage.impl.Hex;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TestStorage extends TestCase  {

    DefaultBlobStorage storage;

    @Override
    protected void setUp() throws Exception {
        File file = File.createTempFile("blob-storage.", ".text");
        file.delete();
        file.mkdir();
        storage = new DefaultBlobStorage(file);
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        FileUtils.deleteTree(storage.getRoot());
        storage = null;
    }

    protected File createTestFile(String content) throws IOException {
        File file = File.createTempFile("blob-storage-testfile.", ".text");
        FileUtils.writeFile(file, content);
        return file;
    }

    protected String getBlobContent(BlobResource res) throws IOException {
        InputStream in = res.getStream();
        try {
            return FileUtils.read(in);
        } finally {
            in.close();
        }
    }

    public void testNoTx() throws Exception {
        // create a new blob
        BlobStorageSession session = storage.getSession();
        BlobResource res1 = doPut(session, "abc");
        BlobResource res2 = session.get(res1.getHash());
        assertEquals(res1, res2);
        assertEquals("abc", getBlobContent(res1));
        assertEquals("abc", getBlobContent(res2));
        assertEquals(res1.lastModified(), res2.lastModified());
        assertEquals(res1.getHash(), res2.getHash());
        String hash1 = res1.getHash();

       // create another blob
        res1 = doPut(session, "efgh");
        res2 = session.get(res1.getHash());
        assertEquals(res1, res2);
        assertEquals("efgh", getBlobContent(res1));
        assertEquals("efgh", getBlobContent(res2));
        assertEquals(res1.lastModified(), res2.lastModified());
        assertEquals(res1.getHash(), res2.getHash());
        String hash2 = res1.getHash();

        // test the 2 blobs are different
        res1 = session.get(hash1);
        res2 = session.get(hash2);
        assertFalse(res1.equals(res2));
        assertEquals("abc", getBlobContent(res1));
        assertEquals("efgh", getBlobContent(res2));
        assertFalse(hash1.equals(hash2));

        // test updating the first blob
        res1 = session.get(hash1);
        res2 = doPut(session, "abc");
        assertEquals(res1, res2);
        assertTrue(res1.lastModified() < res2.lastModified());

        // test remove first blob
        res1 = session.get(hash1);
        assertNotNull(res1);
        session.remove(hash1);
        res1 = session.get(hash1);
        assertNull(res1);

        // test dummy get
        res1 = session.get("xxx-xxx-xxx");
        assertNull(res1);
    }

    protected BlobResource doPut(BlobStorageSession session, String content) throws Exception {
        BlobResource res = null;
        File f = createTestFile(content);
        FileInputStream in = new FileInputStream(f);
        try {
            res = session.put(in);
        } finally {
            in.close();
        }
        return res;
    }

    protected void doRemove(String hash) throws Exception {

    }

    public void testToHex() throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(new byte[] {11, 34, 56} );
        byte[] bytes = md.digest();
        String str1 = new String(Hex.encodeHex(bytes));
       //System.out.println( str1 );

       md = MessageDigest.getInstance("MD5");
       md.update(new byte[] {11, 34, 36} );
       bytes = md.digest();
       String str2 = new String(Hex.encodeHex(bytes));
       //System.out.println( str2 );

       md = MessageDigest.getInstance("MD5");
       md.update(new byte[] {11, 34, 56} );
       bytes = md.digest();
       String str3 = Hex.toHexString(bytes);
       //System.out.println( new String(str3) );

       md = MessageDigest.getInstance("MD5");
       md.update(new byte[] {11, 34, 36} );
       bytes = md.digest();
       String str4 = Hex.toHexString(bytes);
       //System.out.println( str4 );

       assertEquals(str1, str3);
       assertEquals(str2, str4);
    }

}
