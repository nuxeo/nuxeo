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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Watermark;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.chronicle.ChronicleLogManager;

/**
 * @since 9.10
 */
public class TestToolsChronicle extends TestTools {

    protected Path basePath;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Override
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
                appender.append(key, new Record(key, value.getBytes("UTF-8"), Watermark.ofNow().getValue(), null ));
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

    @Override
    public void testPosition() {
        super.testPosition();
        try {
            run(String.format("position %s --log-name %s --group anotherGroup --to-timestamp %s", getManagerOptions(), LOG_NAME, Instant.now().minus(1, ChronoUnit.HOURS)));
            fail();
        } catch (UnsupportedOperationException uoe) {
            assertTrue(uoe.getMessage().contains("does not support seek by timestamp"));
        }
    }

    @Override
    public String getManagerOptions() {
        return String.format("--chronicle %s", basePath);
    }
}
