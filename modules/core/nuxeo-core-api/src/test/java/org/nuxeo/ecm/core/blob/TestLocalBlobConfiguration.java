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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.Environment;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestLocalBlobConfiguration {

    @Test
    public void testPathDefault() throws IOException {
        Map<String, String> properties = Collections.emptyMap();
        LocalBlobStoreConfiguration config = new LocalBlobStoreConfiguration(properties);
        String path = config.storageDir.toString().replace(File.separatorChar, '/');
        assertTrue(path, path.endsWith("/binaries/data"));
    }

    @Test
    public void testPathRelative() throws IOException {
        Map<String, String> properties = Collections.singletonMap("path", "foo");
        LocalBlobStoreConfiguration config = new LocalBlobStoreConfiguration(properties);
        String path = config.storageDir.toString().replace(File.separatorChar, '/');
        assertTrue(path, path.endsWith("/foo/data"));
    }

    @Test
    public void testPathAbsolute() throws IOException {
        Path dir = Environment.getDefault().getData().toPath();
        Map<String, String> properties = Collections.singletonMap("path", dir.resolve("foo").toString());
        LocalBlobStoreConfiguration config = new LocalBlobStoreConfiguration(properties);
        String path = config.storageDir.toString().replace(File.separatorChar, '/');
        assertTrue(path, path.endsWith("/foo/data"));
    }

    @Test
    public void testPathDotDot() throws IOException {
        // make sure the "binaries" base path doesn't exist
        File oldbinaries = new File(Environment.getDefault().getData(), "binaries");
        FileUtils.deleteDirectory(oldbinaries);
        // put binaries at a path that has to be canonicalized
        Map<String, String> properties = Collections.singletonMap("path", "binaries/../newbinaries");
        LocalBlobStoreConfiguration config = new LocalBlobStoreConfiguration(properties);
        String path = config.storageDir.toString().replace(File.separatorChar, '/');
        assertTrue(path, path.endsWith("/newbinaries/data"));
    }

    @Test
    public void testNamespace() throws IOException {
        Map<String, String> properties = Collections.singletonMap("namespace", "myns");
        LocalBlobStoreConfiguration config = new LocalBlobStoreConfiguration(properties);
        String path = config.storageDir.toString().replace(File.separatorChar, '/');
        assertTrue(path, path.endsWith("/binaries_myns/data"));
    }

}
