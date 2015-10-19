/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */

package org.nuxeo.transientstore.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.transientstore.AbstractTransientStore;
import org.nuxeo.ecm.core.transientstore.SimpleTransientStore;
import org.nuxeo.ecm.core.transientstore.TransientStorageGCTrigger;
import org.nuxeo.ecm.core.transientstore.api.MaximumTransientSpaceExceeded;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(TransientStoreFeature.class)
@Deploy("org.nuxeo.ecm.core.event")
public class TransientStorageComplianceFixture {

    @Test
    public void verifyServiceDeclared() throws Exception {

        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        assertNotNull(tss);

        TransientStore ts = tss.getStore("testStore");
        assertNotNull(ts);

        TransientStore ts2 = tss.getStore("microStore");
        assertNotNull(ts2);

        TransientStore ts3 = tss.getStore("miniStore");
        assertNotNull(ts3);

    }

    protected void putEntry(TransientStore ts, String id) {
        ts.putParameter(id, "A", String.valueOf(1));
        ts.putParameter(id, "B", "b");
        String content = "FakeContent";
        Blob blob = new StringBlob(content);
        blob.setFilename("fake.txt");
        blob.setMimeType("text/plain");
        blob.setDigest(DigestUtils.md5Hex(content));
        ts.putBlobs(id, Collections.singletonList(blob));
    }

    @Test
    public void verifyStorage() throws Exception {

        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        TransientStore ts = tss.getStore("testStore");

        long size = ((AbstractTransientStore) ts).getStorageSize();
        assertEquals(0, size);

        putEntry(ts, "1");

        // check FS
        File cacheDir = ((AbstractTransientStore) ts).getCachingDirectory("1");
        assertTrue(cacheDir.exists());
        File[] cacheEntries = cacheDir.listFiles();
        assertTrue(cacheEntries.length > 0);

        // check that entry is stored
        assertTrue(ts.exists("1"));
        assertFalse(ts.isCompleted("1"));
        assertEquals(11, ts.getSize("1"));
        assertEquals("1", ts.getParameter("1", "A"));
        assertEquals("b", ts.getParameter("1", "B"));
        List<Blob> blobs = ts.getBlobs("1");
        assertEquals(1, blobs.size());
        Blob blob = blobs.get(0);
        assertEquals("fake.txt", blob.getFilename());
        assertEquals("text/plain", blob.getMimeType());
        assertEquals(DigestUtils.md5Hex("FakeContent"), blob.getDigest());
        assertEquals("FakeContent", IOUtils.toString(blob.getStream()));

        size = ((AbstractTransientStore) ts).getStorageSize();
        assertEquals(11, size);

        // update the entry
        Blob otherBlob = new StringBlob("FakeContent2");
        otherBlob.setFilename("fake2.txt");
        otherBlob.setMimeType("text/plain");
        blobs.add(otherBlob);
        ts.putBlobs("1", blobs);

        // check update
        assertTrue(ts.exists("1"));
        assertEquals(23, ts.getSize("1"));
        blobs = ts.getBlobs("1");
        assertEquals(2, blobs.size());
        assertEquals("fake.txt", blobs.get(0).getFilename());
        assertEquals("fake2.txt", blobs.get(1).getFilename());
        size = ((AbstractTransientStore) ts).getStorageSize();
        assertEquals(23, size);

        // move to deletable entries
        // check that still here
        ts.release("1");
        assertTrue(ts.exists("1"));

        // check Remove
        ts.remove("1");
        assertFalse(ts.exists("1"));

        size = ((AbstractTransientStore) ts).getStorageSize();
        assertEquals(0, size);
    }

    @Test
    public void verifyNullCases() throws Exception {

        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        TransientStore ts = tss.getStore("testStore");

        assertFalse(ts.exists("fakeEntry"));
        assertNull(ts.getParameters("fakeEntry"));
        assertNull(ts.getParameter("fakeEntry", "fakeParameter"));
        assertNull(ts.getBlobs("fakeEntry"));
        assertEquals(-1, ts.getSize("fakeEntry"));
        assertFalse(ts.isCompleted("fakeEntry"));

        ts.putParameter("testEntry", "param1", "value");
        assertNull(ts.getParameter("testEntry", "param2"));
        assertTrue(ts.getBlobs("testEntry").isEmpty());
    }

    @Test(expected = MaximumTransientSpaceExceeded.class)
    public void verifyMaxSizeException() throws Exception {
        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        TransientStore ts = tss.getStore("microStore");
        putEntry(ts, "A");
    }

    @Test
    public void verifyDeleteAfterUse() throws Exception {

        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        TransientStore ts = tss.getStore("miniStore");
        putEntry(ts, "1");

        // check that entry is stored
        assertTrue(ts.exists("1"));
        assertEquals("1", ts.getParameter("1", "A"));
        assertEquals("b", ts.getParameter("1", "B"));
        List<Blob> blobs = ts.getBlobs("1");
        assertEquals(1, blobs.size());
        Blob blob = blobs.get(0);
        assertEquals("fake.txt", blob.getFilename());
        assertEquals("text/plain", blob.getMimeType());
        assertEquals("FakeContent", IOUtils.toString(blob.getStream()));

        // move to deletable entries
        // check that deleted
        ts.release("1");

        assertFalse(ts.exists("1"));
    }

    @Test
    public void verifyDeleteOnGC() throws Exception {

        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        TransientStore ts = tss.getStore("testStore");
        putEntry(ts, "X");

        // check that entry is stored
        assertTrue(ts.exists("X"));

        // check FS
        File cacheDir = ((AbstractTransientStore) ts).getCachingDirectory("X");
        assertTrue(cacheDir.exists());
        File[] cacheEntries = cacheDir.listFiles();
        assertTrue(cacheEntries.length > 0);

        // do GC for no reason
        ts.doGC();

        // entry is still here
        assertTrue(ts.exists("X"));

        // file is still here
        cacheEntries = cacheDir.listFiles();
        assertTrue(cacheEntries.length > 0);

        // no remove the entry
        ts.remove("X");

        // do GC
        ts.doGC();

        // entry is gone
        assertFalse(ts.exists("X"));

        // cache dir is gone
        assertFalse(cacheDir.exists());
    }

    @Test
    public void verifyDeleteOnGCEvent() throws Exception {

        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        TransientStore ts = tss.getStore("testStore");
        putEntry(ts, "X");

        // check that entry is stored
        assertTrue(ts.exists("X"));

        // check FS
        File cacheDir = ((AbstractTransientStore) ts).getCachingDirectory("X");
        assertTrue(cacheDir.exists());
        File[] cacheEntries = cacheDir.listFiles();
        assertTrue(cacheEntries.length > 0);

        // now remove the entry
        ts.remove("X");

        EventContext evtCtx = new EventContextImpl();
        Framework.getService(EventService.class).fireEvent(TransientStorageGCTrigger.EVENT, evtCtx);

        Thread.sleep(100);
        Framework.getService(EventService.class).waitForAsyncCompletion();

        // entry is gone
        assertFalse(ts.exists("X"));

        // cache dir is gone
        assertFalse(cacheDir.exists());
    }

    @Test
    public void verifyDeleteAfterUseGC() throws Exception {

        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        TransientStore ts = tss.getStore("testStore");
        putEntry(ts, "XXX");

        // check that entry is stored
        assertTrue(ts.exists("XXX"));
        if (ts instanceof SimpleTransientStore) {
            assertNotNull(((SimpleTransientStore) ts).getL1Cache().get("XXX"));
        }

        // move to deletable entries
        // check that still here
        ts.release("XXX");

        assertTrue(ts.exists("XXX"));
        if (ts instanceof SimpleTransientStore) {
            assertNull(((SimpleTransientStore) ts).getL1Cache().get("XXX"));
            assertNotNull(((SimpleTransientStore) ts).getL2Cache().get("XXX"));
        }

        // do GC
        ts.doGC();

        // check still here
        assertTrue(ts.exists("XXX"));

        // empty the L2 cache for the in-memory implementation or remove entry for the Redis one
        if (ts instanceof SimpleTransientStore) {
            ((SimpleTransientStore) ts).getL2Cache().invalidate("XXX");
        } else {
            ts.remove("XXX");
        }

        // do GC
        ts.doGC();

        // check no longer there
        assertFalse(ts.exists("XXX"));
    }

}
