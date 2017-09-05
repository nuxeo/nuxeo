/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.rendition.service.lazy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreProvider;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.lazy.AbstractLazyCachableRenditionProvider;
import org.nuxeo.ecm.platform.rendition.service.RenditionFeature;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.transientstore.test.TransientStoreFeature;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ RenditionFeature.class, TransientStoreFeature.class })
@LocalDeploy("org.nuxeo.ecm.platform.rendition.core:test-lazy-rendition-contrib.xml")
/**
 *
 * Check that LazyRendition work via Nuxeo native API
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class TestLazyRenditions {

    @Inject
    RenditionService rs;

    @Inject
    CoreSession session;

    @AfterClass
    public static void cleanup() throws Exception {
        //
    }

    @Test
    public void testRenditions() throws Exception {
        assertNotNull(rs);
        Rendition rendition = rs.getRendition(session.getRootDocument(), "iamlazy");
        assertNotNull(rendition);
        Blob blob = rendition.getBlob();
        assertEquals(0, blob.getLength());
        assertTrue(blob.getMimeType().contains("empty=true"));
        Thread.sleep(1000);
        Framework.getService(EventService.class).waitForAsyncCompletion(5000);

        rendition = rs.getRendition(session.getRootDocument(), "iamlazy");
        blob = rendition.getBlob();
        assertFalse(blob.getMimeType().contains("empty=true"));
        Calendar modificationDate = rendition.getModificationDate();
        rendition = rs.getRendition(session.getRootDocument(), "iamlazy");
        blob = rendition.getBlob();
        assertFalse(blob.getMimeType().contains("empty=true"));
        assertEquals(modificationDate, rendition.getModificationDate());
        String data = IOUtils.toString(blob.getStream());
        assertEquals("I am really lazy", data);
        assertNotEquals(17, blob.getLength());

        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        TransientStore ts = tss.getStore(AbstractLazyCachableRenditionProvider.CACHE_NAME);
        TransientStoreProvider tsm = (TransientStoreProvider) ts;

        // let's pretend TTL has expired and run GC
        tsm.removeAll();

        // re ask for the rendition : it should not be here anymore
        rendition = rs.getRendition(session.getRootDocument(), "iamlazy");
        blob = rendition.getBlob();
        assertEquals(0, blob.getLength());
        assertTrue(blob.getMimeType().contains("empty=true"));
    }

}
