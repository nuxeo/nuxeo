/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.convert.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.convert.cache.CachableBlobHolder;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;

import junit.framework.TestCase;

public class TestBlobHolderPersistence extends TestCase {

    public void testPersistence() throws Exception {
        List<Blob> blobs = new ArrayList<Blob>();
        for (int i = 0; i < 10; i++) {
            Blob blob = new StringBlob("FileContent_" + i);
            if (i == 0) {
                blob.setFilename("index.html");
            } else {
                blob.setFilename("subFile" + i + ".txt");
            }
            blobs.add(blob);
        }

        CachableBlobHolder holder = new SimpleCachableBlobHolder(blobs);

        String storagePath = System.getProperty("java.io.tmpdir");

        String persistedPath = holder.persist(storagePath);

        // check persistence
        assertNotNull(persistedPath);
        assertTrue(persistedPath.startsWith(storagePath));

        File holderDir = new File(persistedPath);
        assertTrue(holderDir.isDirectory());

        File[] files = holderDir.listFiles();
        assertEquals(2, files.length);

        boolean mainFileFound = false;
        boolean subFilesFound = false;
        for (File file : files) {
            if (file.isDirectory()) {
                File[] subFiles = file.listFiles();
                assertEquals(9, subFiles.length);
                for (File subFile : subFiles) {
                    assertTrue(subFile.getName().startsWith("subFile"));
                    assertTrue(new FileBlob(subFile).getString().startsWith("FileContent_"));
                }
                subFilesFound = true;
            } else {
                assertTrue(file.getName().startsWith("index.html"));
                assertTrue(new FileBlob(file).getString().startsWith("FileContent_"));
                mainFileFound = true;
            }
        }
        assertTrue(mainFileFound);
        assertTrue(subFilesFound);

        // check reload
        holder = new SimpleCachableBlobHolder();
        holder.load(persistedPath);
        assertNotNull(holder.getBlobs());
        assertNotNull(holder.getBlob());

        Blob mainBlob = holder.getBlob();
        assertEquals("index.html", mainBlob.getFilename());
        assertTrue(mainBlob.getString().startsWith("FileContent_0"));

        List<Blob> subBlobs = holder.getBlobs();
        mainBlob = subBlobs.remove(0);

        for (Blob subBlob : subBlobs) {
            assertTrue(subBlob.getFilename().startsWith("subFile"));
            assertTrue(subBlob.getString().startsWith("FileContent_"));
        }
    }

}
