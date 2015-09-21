/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.transientstore.test.work;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.transientstore.api.StorageEntry;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.ecm.core.transientstore.work.TransientStoreWork;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.transientstore.test.InMemoryTransientStoreFeature;

import com.google.inject.Inject;

/**
 * @since 7.4
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, InMemoryTransientStoreFeature.class })
@Deploy("org.nuxeo.ecm.core.event")
public class TestTransientStoreWork {

    @Inject
    protected EventService eventService;

    @Inject
    protected WorkManager workManager;

    @Inject
    protected TransientStoreService transientStoreService;

    @Test
    public void shouldNotHaveResultForNonExistingWork() {
        assertNull(workManager.findResult("nonExistingId"));
    }

    @Test
    public void shouldNotHaveResultForNonTransientStoreWork() {
        Work work = new DummyWork();
        workManager.schedule(work);

        eventService.waitForAsyncCompletion();

        assertNull(workManager.findResult(work.getId()));
    }

    @Test
    public void shouldHaveResultForTransientStoreWork() {
        Work work = new DummyTransientStoreWork();
        workManager.schedule(work);

        eventService.waitForAsyncCompletion();

        String result = workManager.findResult(work.getId());
        assertNotNull(result);
        assertEquals(work.getId() + TransientStoreWork.KEY_SUFFIX, result);
    }

    @Test
    public void shouldStoreBlobAndParamsInTransientStore() throws IOException {
        Work work = new DummyTransientStoreWork();
        workManager.schedule(work);

        eventService.waitForAsyncCompletion();

        String result = workManager.findResult(work.getId());
        assertNotNull(result);

        TransientStoreService transientStoreService = Framework.getService(TransientStoreService.class);
        TransientStore transientStore = transientStoreService.getStore(TransientStoreWork.STORE_NAME);
        StorageEntry entry = transientStore.get(result);
        assertEquals(2, entry.getParameters().size());
        String value = (String) entry.get("firstparam");
        assertNotNull(value);
        assertEquals("firstvalue", value);
        value = (String) entry.get("secondparam");
        assertNotNull(value);
        assertEquals("secondvalue", value);

        List<Blob> blobs = entry.getBlobs();
        assertEquals(1, blobs.size());
        Blob blob = blobs.get(0);
        assertEquals("a simple blob", blob.getString());
        assertEquals("text/plain", blob.getMimeType());
    }
}
