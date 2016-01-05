/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.core.convert.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.nuxeo.common.Environment;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.convert.cache.CachableBlobHolder;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(ConvertFeature.class)
public class TestBlobHolderPersistence {

    @Test
    public void testPersistence() throws Exception {
        List<Blob> blobs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Blob blob = Blobs.createBlob("FileContent_" + i);
            if (i == 0) {
                blob.setFilename("index.html");
            } else {
                blob.setFilename("subFile" + i + ".txt");
            }
            blobs.add(blob);
        }

        CachableBlobHolder holder = new SimpleCachableBlobHolder(blobs);
        String storagePath = Environment.getDefault().getTemp().getPath();
        String persistedPath = holder.persist(storagePath);

        // check persistence
        assertNotNull(persistedPath);
        assertTrue(String.format("%s must start with %s", persistedPath, storagePath),
                persistedPath.startsWith(storagePath));

        File holderDir = new File(persistedPath);
        assertTrue(holderDir.isDirectory());

        File[] files = holderDir.listFiles();
        assertEquals(10, files.length);

        boolean mainFileFound = false;
        for (File file : files) {
            if (file.getName().startsWith("index.html")) {
                mainFileFound = true;
            } else {
                assertTrue(Blobs.createBlob(file).getString().startsWith("FileContent_"));
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
