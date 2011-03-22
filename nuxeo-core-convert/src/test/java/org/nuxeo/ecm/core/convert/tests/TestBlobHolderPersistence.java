/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import junit.framework.TestCase;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.convert.cache.CachableBlobHolder;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;

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
        assertEquals(10, files.length);

        boolean mainFileFound = false;
        for (File file : files) {
            if (file.getName().startsWith("index.html")) {
                mainFileFound = true;
            } else {
                assertTrue(new FileBlob(file).getString().startsWith("FileContent_"));
            }

        }
        assertTrue(mainFileFound);

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
