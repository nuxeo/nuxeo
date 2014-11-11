/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Mathieu Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestXORBinaryManager extends NXRuntimeTestCase {

    private static final String CONTENT = "this is a file au caf\u00e9";

    private static final String CONTENT_MD5 = "d25ea4f4642073b7f218024d397dbaef";

    private static final String XOR_MD5 = "44b0b13c0d30fd15d09f0876be66c56d";

    @Test
    public void testXORBinaryManagerWithInputStream() throws Exception {
        XORBinaryManager binaryManager = new XORBinaryManager();
        binaryManager.initialize(new RepositoryDescriptor());
        assertEquals(0, countFiles(binaryManager.getStorageDir()));

        Binary binary = binaryManager.getBinary(CONTENT_MD5);
        assertNull(binary);

        // store binary
        byte[] bytes = CONTENT.getBytes("UTF-8");
        binary = binaryManager.getBinary(new ByteArrayInputStream(bytes));
        assertNotNull(binary);
        assertEquals(1, countFiles(binaryManager.getStorageDir()));

        // get binary
        binary = binaryManager.getBinary(CONTENT_MD5);
        assertNull(binary);
        binary = binaryManager.getBinary(XOR_MD5);
        assertNotNull(binary);
        assertEquals(bytes.length, binary.getLength());
        assertEquals(CONTENT, IOUtils.toString(binary.getStream(), "UTF-8"));
    }

    protected static int countFiles(File dir) {
        int n = 0;
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                n += countFiles(f);
            } else {
                n++;
            }
        }
        return n;
    }

}
