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
package org.nuxeo.runtime.stream.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.time.Duration;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.runtime.stream.StreamService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @since 9.3
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.runtime.stream")
@Deploy("org.nuxeo.runtime.stream:test-stream-contrib.xml")
public class TestStreamService {

    @Inject
    public StreamService service;

    @Test
    public void testLogManagerAccess() {
        assertNotNull(service);

        LogManager manager = service.getLogManager("default");
        assertNotNull(manager);

        manager = service.getLogManager("import");
        assertNotNull(manager);

        try {
            service.getLogManager("unknown");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // expected
        }

        manager = service.getLogManager("default");
        assertNotNull(manager);

        manager.exists("input");
        assertEquals(1, manager.size("input"));
    }

    @Test
    public void testBasicLogUsage() throws Exception {
        LogManager manager = service.getLogManager("default");
        String logName = "myLog";
        String key = "a key";
        String value = "a value";

        LogAppender<Record> appender = manager.getAppender(logName);
        appender.append(key, Record.of(key, value.getBytes("UTF-8")));

        try (LogTailer<Record> tailer = manager.createTailer("myGroup", logName)) {
            LogRecord<Record> logRecord = tailer.read(Duration.ofSeconds(1));
            assertNotNull(logRecord);
            assertEquals(key, logRecord.message().getKey());
            assertEquals(value, new String(logRecord.message().getData(), "UTF-8"));
        }
        // never close the manager this is done by the service
    }

    @Test
    public void testStreamProcessor() throws Exception {
        LogManager manager = service.getLogManager("default");
        LogAppender<Record> appender = manager.getAppender("input");
        LogTailer<Record> tailer = manager.createTailer("counter", "output");

        // add an input message
        String key = "a key";
        String value = "a value";
        appender.append(key, Record.of(key, value.getBytes("UTF-8")));

        // the computation should forward this message to the output
        LogRecord<Record> logRecord = tailer.read(Duration.ofSeconds(1));
        assertNotNull("Record not found in output stream", logRecord);
        assertEquals(key, logRecord.message().getKey());
        assertEquals(value, new String(logRecord.message().getData(), "UTF-8"));
    }
}
