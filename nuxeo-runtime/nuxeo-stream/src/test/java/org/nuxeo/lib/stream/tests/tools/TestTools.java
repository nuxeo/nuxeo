/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.lib.stream.tests.tools;

import java.nio.file.Path;
import java.time.Duration;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.chronicle.ChronicleLogManager;
import org.nuxeo.lib.stream.tools.Main;

/**
 * @since 9.3
 */
public class TestTools {
    protected static final int NB_RECORD = 50;

    protected static final String LOG_NAME = "myLog";

    protected Path basePath;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void createContent() throws Exception {
        if (basePath != null) {
            return;
        }
        basePath = folder.newFolder().toPath();
        try (LogManager manager = new ChronicleLogManager(basePath, "1m")) {
            manager.createIfNotExists(LOG_NAME, 1);
            LogAppender<Record> appender = manager.getAppender(LOG_NAME);
            for (int i = 0; i < NB_RECORD; i++) {
                String key = "key" + i;
                String value = "Some value for " + i;
                appender.append(key, Record.of(key, value.getBytes("UTF-8")));
            }
            LogTailer<Record> tailer = manager.createTailer("aGroup", LOG_NAME);
            tailer.read(Duration.ofMillis(10));
            tailer.read(Duration.ofMillis(10));
            tailer.commit();
            tailer = manager.createTailer("anotherGroup", LOG_NAME);
            tailer.read(Duration.ofMillis(10));
            tailer.commit();
        }

    }

    @Test
    public void testCat() throws Exception {
        run(String.format("%s cat --name %s --lines 4", basePath, LOG_NAME));
    }

    @Test
    public void testCatWithGroup() throws Exception {
        run(String.format("%s cat --name %s -n 1 --group aGroup", basePath, LOG_NAME));
    }

    @Test
    public void testCatMd() throws Exception {
        run(String.format("%s cat --name %s -n 4 --render markdown", basePath, LOG_NAME));
    }

    @Test
    public void testTail() throws Exception {
        run(String.format("%s tail --name %s -n 5 --render text", basePath, LOG_NAME));
    }

    @Test
    public void testTailAndFollow() throws Exception {
        run(String.format("%s tail -f --name %s -n 2 --render text --timeout 1", basePath, LOG_NAME));
    }

    @Test
    public void testLag() throws Exception {
        run(String.format("%s lag", basePath));
    }

    @Test
    public void testLagForLog() throws Exception {
        run(String.format("%s lag --name %s", basePath, LOG_NAME));
    }

    @Test
    public void testReset() throws Exception {
        run(String.format("%s reset --name %s --group anotherGroup", basePath, LOG_NAME));
    }

    protected void run(String commandLine) {
        String[] args = commandLine.split(" ");
        new Main().run(args);
    }

}
