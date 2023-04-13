/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.core.blob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.nuxeo.ecm.core.blob.scroll.RepositoryBlobScroll.SCROLL_NAME;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.scroll.Scroll;
import org.nuxeo.ecm.core.api.scroll.ScrollRequest;
import org.nuxeo.ecm.core.api.scroll.ScrollService;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.scroll.GenericScrollRequest;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 2023
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestRepositoryBlobScroll {

    protected final static String CONTENT = "hello world";

    protected final static int SIZE = 3;

    protected final static int NB_FILE = SIZE * 4 - 1;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected ScrollService scrollService;

    @Inject
    protected RepositoryService repositoryService;

    protected List<String> expected = new ArrayList<>();

    @Before
    public void createDocuments() {
        for (int i = 0; i < NB_FILE; i++) {
            DocumentModel doc = session.createDocumentModel("/", "doc" + i, "File");
            doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob(CONTENT + i));
            if (i % 3 == 0) {
                doc.setPropertyValue("dc:source", "foo");
            }
            doc = session.createDocument(doc);
            expected.add(((ManagedBlob) doc.getPropertyValue("file:content")).getKey());
        }
        Collections.sort(expected);
        coreFeature.waitForAsyncCompletion();
    }

    protected String getProviderId() {
        return "test";
    }

    protected boolean hasPrefixedKeys() {
        return false;
    }

    protected void test() {
        ScrollRequest request = GenericScrollRequest.builder(SCROLL_NAME, "test").size(SIZE).build();
        assertTrue(scrollService.exists(request));
        try (Scroll scroll = scrollService.scroll(request)) {
            List<String> actual = new ArrayList<>();
            assertNotNull(scroll);
            int i = 0;
            do {
                assertTrue(scroll.hasNext());
                List<String> next = scroll.next();
                assertTrue("Unexpected scolled blobs", i + next.size() <= NB_FILE);
                actual.addAll(next);
                i += next.size();
            } while (i < NB_FILE);
            Collections.sort(actual);
            assertEquals("Unexpected scolled blobs", expected, actual);
            assertFalse(scroll.hasNext());
            assertThrows("Should not be able to scroll beyond limit.", NoSuchElementException.class,
                    () -> scroll.next());
        }
    }

    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/blobGC/test-blob-dispatcher-delete.xml")
    @Test
    public void testRepositoryBlobScrollMultiProvider() {
        test();
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/blobGC/test-blob-cross-repo-provider-delete.xml")
    public void testRepositoryBlobScrollMultiRepoSharedProvider() {
        assumeTrue("MongoDB test only", !coreFeature.getStorageConfiguration().isVCS());
        ScrollRequest request = GenericScrollRequest.builder(SCROLL_NAME, "test").size(SIZE).build();
        assertTrue(scrollService.exists(request));
        assertThrows("Should not be able to scroll in a multi repo with shared provider config.",
                UnsupportedOperationException.class, () -> scrollService.scroll(request));
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/blobGC/test-blob-delete.xml")
    public void testRepositoryBlobScrollSingleRepository() {
        test();
    }

}
