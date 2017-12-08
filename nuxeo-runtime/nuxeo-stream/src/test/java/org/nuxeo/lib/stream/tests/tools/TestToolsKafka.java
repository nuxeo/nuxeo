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

import java.nio.file.Paths;
import java.time.Duration;

import org.junit.BeforeClass;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.kafka.KafkaLogManager;
import org.nuxeo.lib.stream.tests.TestKafkaUtils;
import org.nuxeo.lib.stream.tools.KafkaConfigParser;

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

    @Override
    public void createContent() throws Exception {
        if (configFile != null) {
            return;
        }
        configFile = this.getClass().getClassLoader().getResource(KAFKA_CONF_FILE).getFile();
        KafkaConfigParser config = new KafkaConfigParser(Paths.get(configFile), KAFKA_CONF);
        try (LogManager manager = new KafkaLogManager(config.getZkServers(), config.getPrefix(),
                config.getProducerProperties(), config.getConsumerProperties())) {
            if (!manager.createIfNotExists(LOG_NAME, 1)) {
                // log already exists and should have some content
                return;
            }
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

    @Override
    public String getManagerOptions() {
        return String.format("--kafka %s --kafka-config %s", configFile, KAFKA_CONF);
    }
}
