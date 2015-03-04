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

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.transientstore.AbstractTransientStore;
import org.nuxeo.ecm.core.transientstore.StorageEntryImpl;
import org.nuxeo.ecm.core.transientstore.TransientStorageGCTrigger;
import org.nuxeo.ecm.core.transientstore.api.MaximumTransientSpaceExceeded;
import org.nuxeo.ecm.core.transientstore.api.StorageEntry;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ TransientStoreFeature.class })
public class TransientStorageComplianceFixture {

    @Inject
    CoreSession coreSession;

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

    protected StorageEntry createEntry(String id) {

        StorageEntry entry = new StorageEntryImpl(id);
        entry.put("A", 1);
        entry.put("B", "b");
        Blob blob = new StringBlob("FakeContent");
        blob.setFilename("fake.txt");
        blob.setMimeType("text/plain");
        entry.addBlob(blob);
        return entry;
    }

    @Test
    public void verifyStorage() throws Exception {

        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        TransientStore ts = tss.getStore("testStore");

        long size = ((AbstractTransientStore)ts).getStorageSize();
        assertEquals(0, size);

        ts.put(createEntry("1"));

        // check FS
        File cacheDir = ((AbstractTransientStore) ts).getCachingDirectory("1");
        assertTrue(cacheDir.exists());
        File[] cacheEntries = cacheDir.listFiles();
        assertTrue(cacheEntries.length > 0);

        // check that entry is stored
        StorageEntry se = ts.get("1");
        assertNotNull(se);
        assertEquals(1, se.get("A"));
        assertEquals("b", se.get("B"));
        assertEquals("fake.txt", se.getBlobs().get(0).getFilename());
        assertEquals("text/plain", se.getBlobs().get(0).getMimeType());
        assertEquals("FakeContent", IOUtils.toString(se.getBlobs().get(0).getStream()));

        size = ((AbstractTransientStore)ts).getStorageSize();
        assertEquals(11, size);

        // move to deletable entries
        // check that still here
        ts.canDelete("1");
        se = ts.get("1");
        assertNotNull(se);

        // check Remove
        ts.remove("1");
        se = ts.get("1");
        assertNull(se);

        size = ((AbstractTransientStore)ts).getStorageSize();
        assertEquals(0, size);

    }

    @Test(expected = MaximumTransientSpaceExceeded.class)
    public void verifyMaxSizeException() throws Exception {
        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        TransientStore ts = tss.getStore("microStore");
        ts.put(createEntry("A"));
    }

    @Test
    public void verifyDeleteAfterUse() throws Exception {

        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        TransientStore ts = tss.getStore("miniStore");
        ts.put(createEntry("1"));

        // check that entry is stored
        StorageEntry se = ts.get("1");
        assertNotNull(se);
        assertEquals(1, se.get("A"));
        assertEquals("b", se.get("B"));
        assertEquals("fake.txt", se.getBlobs().get(0).getFilename());
        assertEquals("text/plain", se.getBlobs().get(0).getMimeType());
        assertEquals("FakeContent", IOUtils.toString(se.getBlobs().get(0).getStream()));

        // move to deletable entries
        // check that still here
        ts.canDelete("1");

        se = ts.get("1");
        assertNull(se);
    }

    @Test
    public void verifyDeleteOnGC() throws Exception {

        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        TransientStore ts = tss.getStore("testStore");
        ts.put(createEntry("X"));

        // check that entry is stored
        StorageEntry se = ts.get("X");
        assertNotNull(se);

        // check FS
        File cacheDir = ((AbstractTransientStore) ts).getCachingDirectory("X");
        assertTrue(cacheDir.exists());
        File[] cacheEntries = cacheDir.listFiles();
        assertTrue(cacheEntries.length > 0);

        // do GC for no reason
        ts.doGC();

        // entry is still here
        se = ts.get("X");
        assertNotNull(se);

        // file is still here
        cacheEntries = cacheDir.listFiles();
        assertTrue(cacheEntries.length > 0);

        // no remove the entry
        ts.remove("X");

        // do GC
        ts.doGC();

        // entry is gone
        se = ts.get("X");
        assertNull(se);

        // cache dir is gone
        assertFalse(cacheDir.exists());
    }

    @Test
    public void verifyDeleteOnGCEvent() throws Exception {

        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        TransientStore ts = tss.getStore("testStore");
        ts.put(createEntry("X"));

        // check that entry is stored
        StorageEntry se = ts.get("X");
        assertNotNull(se);

        // check FS
        File cacheDir = ((AbstractTransientStore) ts).getCachingDirectory("X");
        assertTrue(cacheDir.exists());
        File[] cacheEntries = cacheDir.listFiles();
        assertTrue(cacheEntries.length > 0);

        // now remove the entry
        ts.remove("X");

        EventContext evtCtx = new EventContextImpl(coreSession);
        Framework.getService(EventService.class).fireEvent(TransientStorageGCTrigger.EVENT, evtCtx);

        Thread.sleep(100);
        Framework.getService(EventService.class).waitForAsyncCompletion();

        // entry is gone
        se = ts.get("X");
        assertNull(se);

        // cache dir is gone
        assertFalse(cacheDir.exists());
    }

}
