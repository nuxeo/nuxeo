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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestPathStrategies {

    private String stringify(Path file) {
        return file.toString().replace(File.separatorChar, '/');
    }

    @Test
    public void testSafeRegex() throws IOException {
        String rejected = "";
        for (char c = ' '; c <= '~'; c++) {
            if (!PathStrategy.SAFE.matcher(String.valueOf(c)).matches()) {
                rejected += c;
            }
        }
        assertEquals("%/:\\", rejected);
    }

    @Test
    public void testSafePath() throws IOException {
        Path dir = Files.createTempDirectory("tmp_");
        PathStrategy ps = new PathStrategyFlat(dir);

        // unchanged
        for (String key : Arrays.asList( //
                "foo.doc", //
                "hello world.bin" //
                )) {
            assertEquals(key, ps.safePath(key));
        }

        // encoded
        String[] table = new String[] {
                ".", "%.", //
                "..", "%..", //
                "a/b", "%a%2fb", //
                "a\\b", "%a%5cb", //
                "100%", "%100%25", //
                "50:50", "%50%3a50", //
                "caf\u00e9", "%caf%c3%a9" //
        };
        for (int i = 0; i < table.length; i += 2) {
            String key = table[i];
            String expected = table[i+1];
            assertEquals(expected, ps.safePath(key));
        }
    }

    @Test
    public void testPathStrategyFlat() throws IOException {
        Path dir = Files.createTempDirectory("tmp_");
        PathStrategy ps = new PathStrategyFlat(dir);

        // valid key
        String path = stringify(ps.getPathForKey("123456789"));
        assertTrue(path, path.endsWith("/123456789"));

        // encoded key
        path = stringify(ps.getPathForKey("../foo"));
        assertTrue(path, path.endsWith("/%..%2ffoo"));
    }

    @Test
    public void testPathStrategySubDirs() throws IOException {
        Path dir = Files.createTempDirectory("tmp_");
        PathStrategy ps = new PathStrategySubDirs(dir, 2);

        // valid key
        String path = stringify(ps.getPathForKey("123456789"));
        assertTrue(path, path.endsWith("/12/34/123456789"));

        // short key
        path = stringify(ps.getPathForKey("abc"));
        assertTrue(path, path.endsWith("/000/abc"));

        // encoded key
        path = stringify(ps.getPathForKey("bad/key"));
        assertTrue(path, path.endsWith("/%b/ad/%bad%2fkey"));
    }

    @Test
    public void testTempFile() throws IOException {
        Path dir = Files.createTempDirectory("tmp_");
        PathStrategy ps = new PathStrategyFlat(dir);
        Path path = ps.createTempFile();
        assertTrue(ps.isTempFile(path));
        // non-temp file
        Path path2 = path.resolveSibling("1234");
        assertFalse(ps.isTempFile(path2));
    }

}
