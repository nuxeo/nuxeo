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
package org.nuxeo.ecm.core.scroll;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.scroll.Scroll;
import org.nuxeo.ecm.core.api.scroll.ScrollRequest;
import org.nuxeo.ecm.core.api.scroll.ScrollService;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreBulkFeature.class)
public class TestScrollService {

    @Inject
    protected ScrollService scrollService;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void testScrollService() {
        assertNotNull(scrollService);
        // default implementations
        assertTrue(scrollService.exists(StaticScrollRequest.builder("").build()));
        assertTrue(scrollService.exists(DocumentScrollRequest.builder("").build()));
        // explicit
        assertTrue(scrollService.exists(DocumentScrollRequest.builder("").name("repository").build()));
        assertFalse(scrollService.exists(DocumentScrollRequest.builder("").name("unknown").build()));
    }

    @Test
    public void testStaticScroll() {
        ScrollRequest request = StaticScrollRequest.builder("single").build();
        try (Scroll scroll = scrollService.scroll(request)) {
            assertNotNull(scroll);
            assertTrue(scroll.hasNext());
            assertEquals(Arrays.asList("single"), scroll.next());
            assertFalse(scroll.hasNext());
        }

        request = StaticScrollRequest.builder("").build();
        try (Scroll scroll = scrollService.scroll(request)) {
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
        try (Scroll scroll = scrollService.scroll(request)) {
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
        try (Scroll scroll = scrollService.scroll(request)) {
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

    @Test
    @Deploy("org.nuxeo.ecm.core.bulk.test:OSGI-INF/test-scroll-contrib.xml")
    public void testMyFileScroll() throws IOException {
        assertNotNull(scrollService);
        int size = 13;
        String file = createFile(size);
        ScrollRequest request = GenericScrollRequest.builder("myFileScroll", file).size(5).build();
        assertTrue(scrollService.exists(request));
        try (Scroll scroll = scrollService.scroll(request)) {
            assertNotNull(scroll);
            assertTrue(scroll.hasNext());
            assertEquals(Arrays.asList("a line 0", "a line 1", "a line 2", "a line 3", "a line 4"), scroll.next());
            assertTrue(scroll.hasNext());
            scroll.next();
            assertTrue(scroll.hasNext());
            assertEquals(Arrays.asList("a line 10", "a line 11", "a line 12"), scroll.next());
            assertFalse(scroll.hasNext());
            try {
                scroll.next();
                fail("Exception expected");
            } catch (NoSuchElementException e) {
                // expected
            }
        }
        request = GenericScrollRequest.builder("myFileScroll", file).size(200).build();
        try (Scroll scroll = scrollService.scroll(request)) {
            assertTrue(scroll.hasNext());
            assertEquals(size, scroll.next().size());
            assertFalse(scroll.hasNext());
        }

    }

    protected String createFile(int numberOfLine) throws IOException {
        File tempFile = testFolder.newFile("file.txt");
        try (FileWriter fw = new FileWriter(tempFile, true); BufferedWriter bw = new BufferedWriter(fw)) {
            for (int i = 0; i < numberOfLine; i++) {
                bw.write("a line " + i);
                bw.newLine();
            }
        }
        return tempFile.getAbsolutePath();
    }

    @Test
    public void testEmptyScroll() {
        ScrollRequest request = EmptyScrollRequest.of();
        assertTrue(scrollService.exists(request));
        try (Scroll scroll = scrollService.scroll(request)) {
            assertNotNull(scroll);
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
