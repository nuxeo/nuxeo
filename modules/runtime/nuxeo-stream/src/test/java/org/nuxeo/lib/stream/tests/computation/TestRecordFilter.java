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
package org.nuxeo.lib.stream.tests.computation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.lib.stream.tests.TestLibChronicle.IS_WIN;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.RecordFilter;
import org.nuxeo.lib.stream.computation.Settings;
import org.nuxeo.lib.stream.computation.StreamManager;
import org.nuxeo.lib.stream.computation.StreamProcessor;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.lib.stream.computation.log.LogStreamManager;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogOffset;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.log.chronicle.ChronicleLogManager;

/**
 * @since 11.1
 */
public class TestRecordFilter {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    protected File basePath;

    @Before
    public void skipWindowsThatDoNotCleanTempFolder() {
        org.junit.Assume.assumeFalse(IS_WIN);
    }

    class SkipFilter implements RecordFilter {

        @Override
        public Record beforeAppend(Record record) {
            if (record.getKey().startsWith("skipMeOnAppend")) {
                return null;
            }
            return record;
        }

        @Override
        public Record afterRead(Record record, LogOffset offset) {
            if (record.getKey().startsWith("skipMeOnRead")) {
                return null;
            }
            return record;
        }
    }

    class ChangeFilter implements RecordFilter {

        @Override
        public Record beforeAppend(Record record) {
            if (record.getKey().startsWith("changeMeOnAppend")) {
                return Record.of(record.getKey().replace("changeMeOnAppend", "changedOnAppend"), record.getData());
            }
            return record;
        }

        @Override
        public Record afterRead(Record record, LogOffset offset) {
            if (record.getKey().startsWith("changeMeOnRead")) {
                return Record.of(record.getKey().replace("changeMeOnRead", "changedOnRead"), record.getData());
            }
            return record;
        }
    }

    public LogManager getLogManager() throws Exception {
        this.basePath = folder.newFolder();
        return new ChronicleLogManager(basePath.toPath());
    }

    @Test
    public void testFilter() throws Exception {
        Topology topology = Topology.builder()
                                    .addComputation(() -> new ComputationForward("C1", 1, 1),
                                            Arrays.asList("i1:input", "o1:output"))
                                    .build();
        Settings settings = new Settings(1, 1);
        ChangeFilter changeFilter = new ChangeFilter();
        settings.addFilter("input", new SkipFilter());
        settings.addFilter("input", changeFilter);

        try (LogManager manager = getLogManager()) {
            StreamManager streamManager = new LogStreamManager(manager);
            StreamProcessor processor = streamManager.registerAndCreateProcessor("processor", topology, settings);

            processor.start();
            streamManager.append("input", Record.of("keepMeLikeThis", null));
            streamManager.append("input", Record.of("skipMeOnAppend", null));
            streamManager.append("input", Record.of("changeMeOnAppend", null));
            streamManager.append("input", Record.of("skipMeOnRead", null));
            streamManager.append("input", Record.of("changeMeOnRead", null));

            assertTrue(processor.drainAndStop(Duration.ofMinutes(1)));
            try (LogTailer<Record> tailer = manager.createTailer(Name.ofUrn("test/group"), Name.ofUrn("output"))) {
                LogRecord<Record> logRecord = tailer.read(Duration.ofSeconds(1));
                assertNotNull(logRecord);
                assertEquals("keepMeLikeThis", logRecord.message().getKey());

                logRecord = tailer.read(Duration.ofSeconds(1));
                assertNotNull(logRecord);
                assertEquals("changedOnAppend", logRecord.message().getKey());

                logRecord = tailer.read(Duration.ofSeconds(1));
                assertNotNull(logRecord);
                assertEquals("changedOnRead", logRecord.message().getKey());

                logRecord = tailer.read(Duration.ofSeconds(1));
                assertNull(logRecord);
            }
        }
    }

}
