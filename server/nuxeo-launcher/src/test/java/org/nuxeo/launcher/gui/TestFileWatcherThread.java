/*
 * (C) Copyright 2022 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.launcher.gui;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Duration.ONE_SECOND;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 2023.0
 */
public class TestFileWatcherThread {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder(new File(FeaturesRunner.getBuildDirectory()));

    // test the case where nxserver/config exists
    @Test
    public void testDirectFileChange() throws Exception {
        // set up the file to watch
        var file = this.folder.newFile("file.txt").toPath();
        // create the watcher thread
        var atomicBoolean = new AtomicBoolean(false);
        new FileWatcherThread(file, () -> atomicBoolean.set(true)).start();

        // wait a bit that the watcher thread starts
        Thread.sleep(100);

        // do a modification on the file
        Files.write(file, List.of("new line"));

        // assert the watcher gets the event
        await().atMost(ONE_SECOND).untilTrue(atomicBoolean);
    }

    // test the case where nxserver/config doesn't exist
    @Test
    public void testSubDirectoryFileChange() throws Exception {
        // set up the file to watch
        var file = this.folder.newFolder("parent").toPath().resolve("config/file.txt");
        // create the watcher thread
        var atomicBoolean = new AtomicBoolean(false);
        new FileWatcherThread(file, () -> atomicBoolean.set(true)).start();

        // wait a bit that the watcher thread starts
        Thread.sleep(100);

        // create the sub directory
        Files.createDirectories(file.getParent());

        // wait that the watcher register the sub directory
        // the watcher service may be long to fire the event
        Thread.sleep(1000);

        // do a modification on the file
        Files.write(file, List.of("new line"));

        // assert the watcher gets the event
        await().atMost(ONE_SECOND).untilTrue(atomicBoolean);
    }

}
