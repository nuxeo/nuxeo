/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.nuxeo.runtime.services.streaming.StreamSource;

/**
 * @author Florent Guillaume
 */
public class TestSQLBackendScrambling extends SQLBackendTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.core.storage.sql.test.tests",
                "OSGI-INF/test-backend-core-types-contrib.xml");
    }

    @Override
    protected RepositoryDescriptor newDescriptor(long clusteringDelay,
            boolean fulltextDisabled) {
        RepositoryDescriptor descriptor = super.newDescriptor(clusteringDelay,
                fulltextDisabled);
        descriptor.binaryManagerClass = XORBinaryManager.class;
        descriptor.binaryManagerKey = "AB";
        return descriptor;
    }

    public void testBinaryScrambling() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node nodea = session.addChildNode(root, "foo", null, "TestDoc", false);

        InputStream in = new ByteArrayInputStream("abc".getBytes("UTF-8"));
        Binary bin = session.getBinary(in);
        assertEquals(3, bin.getLength());
        // the standard md5 of "abc" is 900150983cd24fb0d6963f7d28e17f72
        assertEquals("70b89fb2ca14332c10e9966d779e5515", bin.getDigest());
        assertEquals("abc", readAllBytes(bin.getStream()));
        assertEquals("abc", readAllBytes(bin.getStream())); // readable twice
        StreamSource streamSource = bin.getStreamSource();
        assertEquals(3, streamSource.getLength());
        assertEquals("abc", readAllBytes(streamSource.getStream()));
        assertEquals("abc", readAllBytes(streamSource.getStream())); // twice
        assertEquals("abc", new String(streamSource.getBytes()));
        assertEquals("abc", streamSource.getString());
        assertTrue(streamSource.canReopen());

        nodea.setSingleProperty("tst:bin", bin);
        session.save();
        session.close();

        // now read from another session
        session = repository.getConnection();
        root = session.getRootNode();
        nodea = session.getChildNode(root, "foo", false);
        SimpleProperty binProp = nodea.getSimpleProperty("tst:bin");
        assertNotNull(binProp);
        Serializable value = binProp.getValue();
        assertTrue(value instanceof Binary);
        bin = (Binary) value;
        in = bin.getStream();
        assertEquals(3, bin.getLength());
        assertEquals("70b89fb2ca14332c10e9966d779e5515", bin.getDigest());
        assertEquals("abc", readAllBytes(bin.getStream()));
        assertEquals("abc", readAllBytes(bin.getStream())); // readable twice
    }

    // assumes one read will read everything
    protected String readAllBytes(InputStream in) throws IOException {
        if (!(in instanceof BufferedInputStream)) {
            in = new BufferedInputStream(in);
        }
        int len = in.available();
        byte[] bytes = new byte[len];
        int read = in.read(bytes);
        assertEquals(len, read);
        assertEquals(-1, in.read()); // EOF
        return new String(bytes, "ISO-8859-1");
    }

}
