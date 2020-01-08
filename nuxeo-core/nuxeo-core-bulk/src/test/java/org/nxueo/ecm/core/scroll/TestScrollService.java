/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nxueo.ecm.core.scroll;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.scroll.Scroll;
import org.nuxeo.ecm.core.api.scroll.ScrollRequest;
import org.nuxeo.ecm.core.api.scroll.ScrollService;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.scroll.DocumentScrollRequest;
import org.nuxeo.ecm.core.scroll.StaticScrollRequest;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreBulkFeature.class)
public class TestScrollService {

    @Inject
    protected ScrollService service;

    @Test
    public void testScrollService() {
        assertNotNull(service);
        // default implementations
        assertTrue(service.exists(StaticScrollRequest.builder("").build()));
        assertTrue(service.exists(DocumentScrollRequest.builder("").build()));
        // explicit
        assertTrue(service.exists(DocumentScrollRequest.builder("").name("repository").build()));
        assertFalse(service.exists(DocumentScrollRequest.builder("").name("unknown").build()));
    }

    @Test
    public void testStaticScroll() {
        ScrollRequest request = StaticScrollRequest.builder("single").build();
        try (Scroll scroll = service.scroll(request)) {
            assertNotNull(scroll);
            assertTrue(scroll.hasNext());
            assertEquals(Arrays.asList("single"), scroll.next());
            assertFalse(scroll.hasNext());
        }

        request = StaticScrollRequest.builder("").build();
        try (Scroll scroll = service.scroll(request)) {
            assertNotNull(scroll);
            assertTrue(scroll.hasNext());
            assertEquals(Arrays.asList(""), scroll.next());
            assertFalse(scroll.hasNext());
        }
    }

    @Test
    public void testStaticScrollNormal() {
        List<String> ids = Arrays.asList("first", "2", "3", "4", "5", "6", "7", "8", "9", "last");
        int size = 4;
        ScrollRequest request = StaticScrollRequest.builder(ids).size(size).build();
        try (Scroll scroll = service.scroll(request)) {
            assertNotNull(scroll);

            assertTrue(scroll.hasNext());
            assertEquals(Arrays.asList("first", "2", "3", "4"), scroll.next());

            assertTrue(scroll.hasNext());
            assertEquals(Arrays.asList("5", "6", "7", "8"), scroll.next());

            assertTrue(scroll.hasNext());
            assertEquals(Arrays.asList("9", "last"), scroll.next());

            assertFalse(scroll.hasNext());
            try {
                scroll.next();
                fail("Exception expected");
            } catch (NoSuchElementException e) {
                // expected
            }
        }
    }

    @Test
    public void testStaticScrollBis() {
        List<String> ids = Arrays.asList("first", "2", "3", "4", "5", "6", "7", "8", "9", "last");
        int size = 4;
        ScrollRequest request = StaticScrollRequest.builder(ids).size(size).build();
        try (Scroll scroll = service.scroll(request)) {
            assertNotNull(scroll);
            assertTrue(scroll.hasNext());
            assertTrue(scroll.hasNext());
            assertTrue(scroll.hasNext());
            assertEquals(Arrays.asList("first", "2", "3", "4"), scroll.next());
            assertEquals(Arrays.asList("5", "6", "7", "8"), scroll.next());
            assertTrue(scroll.hasNext());
            assertTrue(scroll.hasNext());
            assertEquals(Arrays.asList("9", "last"), scroll.next());
            assertFalse(scroll.hasNext());
            assertFalse(scroll.hasNext());
            try {
                scroll.next();
                fail("Exception expected");
            } catch (NoSuchElementException e) {
                // expected
            }
        }
    }

}
