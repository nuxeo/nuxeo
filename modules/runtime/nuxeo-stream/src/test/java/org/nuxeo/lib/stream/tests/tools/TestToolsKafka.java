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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.nuxeo.lib.stream.tests.log.TestLog.DEF_TIMEOUT;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import org.junit.BeforeClass;
import org.junit.Test;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Watermark;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.tests.TestKafkaUtils;

/**
 * @since 9.10
 */
public class TestToolsKafka extends TestTools {

    protected static final String KAFKA_CONF_FILE = "kafka-tools-config.xml";

    protected static final String KAFKA_CONF = "default";

    protected String configFile;

    @BeforeClass
    public static void assumeKafkaEnabled() {
        TestKafkaUtils.assumeKafkaEnabled();

    }

    @Test
    public void testPositionAfterDate() throws InterruptedException {
        runShouldFail(String.format("position %s --log-name %s --group test/anotherGroup --after-date %s",
                getManagerOptions(), LOG_NAME, Instant.now().plus(1, ChronoUnit.HOURS)));
        // move to target timestamp
        run(String.format("position %s --log-name %s --group test/anotherGroup --after-date %s", getManagerOptions(),
                LOG_NAME, Instant.ofEpochMilli(Watermark.ofValue(targetRecord.getWatermark()).getTimestamp())));
        // open a tailer with the moved group we should be on the same record
        try (LogTailer<Record> tailer = getManager().createTailer(Name.ofUrn("test/anotherGroup"),
                Name.ofUrn(LOG_NAME))) {
            LogRecord<Record> rec = tailer.read(DEF_TIMEOUT);
            assertNotNull(rec);
            assertEquals(targetRecord, rec.message());
        }
    }

    @Override
    public String getManagerOptions() {
        return String.format("--kafka %s --kafka-config %s", getConfigFile(), KAFKA_CONF);
    }

    protected String getConfigFile() {
        if (configFile == null) {
            configFile = Objects.requireNonNull(this.getClass().getClassLoader().getResource(KAFKA_CONF_FILE))
                                .getFile();
        }
        return configFile;
    }
}
