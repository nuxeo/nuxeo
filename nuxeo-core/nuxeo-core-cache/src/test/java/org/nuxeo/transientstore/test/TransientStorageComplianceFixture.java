/*
 * (C) Copyright 2015-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
package org.nuxeo.transientstore.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.Environment;
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
import org.nuxeo.ecm.core.transientstore.api.TransientStoreProvider;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(TransientStoreFeature.class)
@Deploy({ //
        "org.nuxeo.ecm.core.event", //
        "org.nuxeo.ecm.core.cache.test:test-in-memory-transientstore-contrib.xml", //
})
public class TransientStorageComplianceFixture {

    @Inject
    HotDeployer deployer;

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
        TransientStoreProvider tsm = (TransientStoreProvider) ts;

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
        assertEquals(1, tsm.keySet().size());
        assertTrue(tsm.keySet().contains("1"));
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
        assertEquals(1, tsm.keySet().size());
        assertTrue(tsm.keySet().contains("1"));
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
        assertEquals(1, tsm.keySet().size());
        assertTrue(tsm.keySet().contains("1"));

        // check Remove
        ts.remove("1");
        assertFalse(ts.exists("1"));
        assertEquals(0, tsm.keySet().size());

        size = ((AbstractTransientStore) ts).getStorageSize();
        assertEquals(0, size);
    }

    @Test
    public void verifyNullCases() throws Exception {

        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        TransientStore ts = tss.getStore("testStore");

        // Non existing entry
        assertFalse(ts.exists("fakeEntry"));
        assertNull(ts.getParameters("fakeEntry"));
        assertNull(ts.getParameter("fakeEntry", "fakeParameter"));
        assertNull(ts.getBlobs("fakeEntry"));
        assertEquals(-1, ts.getSize("fakeEntry"));
        assertFalse(ts.isCompleted("fakeEntry"));

        // Entry with parameters only
        ts.putParameter("testEntry", "param1", "value");
        assertTrue(ts.exists("testEntry"));
        Map<String, Serializable> params = ts.getParameters("testEntry");
        assertNotNull(params);
        assertEquals(1, params.size());
        assertNotNull(ts.getParameter("testEntry", "param1"));
        assertNull(ts.getParameter("testEntry", "param2"));
        List<Blob> blobs = ts.getBlobs("testEntry");
        assertNotNull(blobs);
        assertTrue(blobs.isEmpty());

        // Entry with blobs only
        ts.putBlobs("otherEntry", Collections.singletonList(new StringBlob("joe")));
        assertTrue(ts.exists("otherEntry"));
        params = ts.getParameters("otherEntry");
        assertNotNull(params);
        assertTrue(params.isEmpty());
        blobs = ts.getBlobs("otherEntry");
        assertNotNull(blobs);
        assertEquals(1, blobs.size());
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
        TransientStoreProvider tsm = (TransientStoreProvider) ts;
        putEntry(ts, "X");

        // check that entry is stored
        assertTrue(ts.exists("X"));

        // check FS
        File cacheDir = ((AbstractTransientStore) ts).getCachingDirectory("X");
        assertTrue(cacheDir.exists());
        File[] cacheEntries = cacheDir.listFiles();
        assertTrue(cacheEntries.length > 0);

        // do GC for no reason
        tsm.doGC();

        // entry is still here
        assertTrue(ts.exists("X"));

        // file is still here
        cacheEntries = cacheDir.listFiles();
        assertTrue(cacheEntries.length > 0);

        // no remove the entry
        ts.remove("X");

        // do GC
        tsm.doGC();

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
        TransientStoreProvider tsm = (TransientStoreProvider) ts;
        putEntry(ts, "foobar");

        // check that entry is stored
        assertTrue(ts.exists("foobar"));
        if (ts instanceof SimpleTransientStore) {
            assertNotNull(((SimpleTransientStore) ts).getL1Cache().getIfPresent("foobar"));
        }

        // move to deletable entries
        // check that still here
        ts.release("foobar");

        assertTrue(ts.exists("foobar"));
        if (ts instanceof SimpleTransientStore) {
            assertNull(((SimpleTransientStore) ts).getL1Cache().getIfPresent("foobar"));
            assertNotNull(((SimpleTransientStore) ts).getL2Cache().getIfPresent("foobar"));
        }

        // do GC
        tsm.doGC();

        // check still here
        assertTrue(ts.exists("foobar"));

        // empty the L2 cache for the in-memory implementation or remove entry for the Redis one
        if (ts instanceof SimpleTransientStore) {
            ((SimpleTransientStore) ts).getL2Cache().invalidate("foobar");
        } else {
            ts.remove("foobar");
        }

        // do GC
        tsm.doGC();

        // check no longer there
        assertFalse(ts.exists("foobar"));
    }

    @Test
    public void verifyStorePathCanBeSpecified() throws Exception {
        Framework.getProperties().setProperty("nuxeo.data.dir", Environment.getDefault().getData().getAbsolutePath());

        TransientStoreService tss = Framework.getService(TransientStoreService.class);

        // Verify default behavior (store cache dir is in ${nuxeo.data.dir}/transientstores/{name}
        AbstractTransientStore ts = (AbstractTransientStore) tss.getStore("microStore");
        assertEquals(Framework.expandVars("${nuxeo.data.dir}/transientstores/microStore"),
                ts.getCacheDir().getAbsolutePath());

        // Verify when a path is given
        deployer.deploy("org.nuxeo.ecm.core.cache.test:testpath-store.xml");
        // need to re-fecth service instance after hot deploy
        tss = Framework.getService(TransientStoreService.class);
        ts = (SimpleTransientStore) tss.getStore("testPath");
        assertEquals(Framework.expandVars("${nuxeo.data.dir}/test"), ts.getCacheDir().getAbsolutePath());

    }

}
