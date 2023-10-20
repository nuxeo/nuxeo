/*
 * (C) Copyright 2023 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 *     Antoine Taillefer
 */
package org.nuxeo.runtime.trackers.files;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileDeleteStrategy;
import org.junit.Test;

/**
 * Tests the {@link FileEventTracker}.
 *
 * @since 2023.5
 */
public class TestFileEventTracker {

    protected FileDeleteStrategy deleteStrategy = FileEventTracker.getDeleteStrategy();

    protected FileDeleteStrategy forceDeleteStrategy = FileEventTracker.getForceDeleteStrategy();

    @Test
    public void testDeleteStrategy() throws IOException {

        // protected file
        File file = Files.createTempFile("foo", "bar").toFile();
        FileEventTracker.registerProtectedPath(file.getPath());
        assertFalse(deleteStrategy.deleteQuietly(file));

        // regular file
        file = Files.createTempFile("foo", "bar").toFile();
        assertTrue(deleteStrategy.deleteQuietly(file));

        // empty directory
        Path directory = Files.createTempDirectory("foo");
        assertTrue(deleteStrategy.deleteQuietly(directory.toFile()));

        // non empty directory, deletion must fail
        directory = Files.createTempDirectory("foo");
        Files.createTempFile(directory, "foo", "bar");
        assertFalse(deleteStrategy.deleteQuietly(directory.toFile()));
    }

    @Test
    public void testForceDeleteStrategy() throws IOException {

        // protected file
        File file = Files.createTempFile("foo", "bar").toFile();
        FileEventTracker.registerProtectedPath(file.getPath());
        assertFalse(forceDeleteStrategy.deleteQuietly(file));

        // regular file
        file = Files.createTempFile("foo", "bar").toFile();
        assertTrue(forceDeleteStrategy.deleteQuietly(file));

        // empty directory
        Path directory = Files.createTempDirectory("foo");
        assertTrue(forceDeleteStrategy.deleteQuietly(directory.toFile()));

        // non empty directory, deletion must succeed
        directory = Files.createTempDirectory("foo");
        Files.createTempFile(directory, "foo", "bar");
        assertTrue(forceDeleteStrategy.deleteQuietly(directory.toFile()));
    }

}
