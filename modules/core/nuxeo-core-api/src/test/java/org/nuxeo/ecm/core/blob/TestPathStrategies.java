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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.NuxeoException;
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
    public void testPathStrategyFlat() throws IOException {
        Path dir = Files.createTempDirectory("tmp_");
        PathStrategy ps = new PathStrategyFlat(dir);
        String path = stringify(ps.getPathForKey("123456789"));
        assertTrue(path, path.endsWith("/123456789"));

        try {
            ps.getPathForKey("../foo");
            fail();
        } catch (NuxeoException e) {
            assertEquals("Invalid key: ../foo", e.getMessage());
        }
    }

    @Test
    public void testPathStrategySubDirs() throws IOException {
        Path dir = Files.createTempDirectory("tmp_");
        PathStrategy ps = new PathStrategySubDirs(dir, 2);

        // valid key
        String path = ps.getPathForKey("123456789").toString().replace(File.separatorChar, '/');
        assertTrue(path, path.endsWith("/12/34/123456789"));

        // short key
        path = stringify(ps.getPathForKey("abc"));
        assertTrue(path, path.endsWith("/000/abc"));

        // invalid key
        try {
            ps.getPathForKey("bad/key");
            fail();
        } catch (NuxeoException e) {
            assertEquals("Invalid key: bad/key", e.getMessage());
        }
    }

}
