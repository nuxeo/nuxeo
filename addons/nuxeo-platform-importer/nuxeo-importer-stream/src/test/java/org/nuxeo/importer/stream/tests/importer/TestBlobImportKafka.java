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

package org.nuxeo.importer.stream.tests.importer;

import org.junit.BeforeClass;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.kafka.KafkaLogManager;
import org.nuxeo.lib.stream.tests.pattern.KafkaHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kafka.KafkaConfigService;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @since 9.2
 */
@LocalDeploy("org.nuxeo.importer.stream:test-kafka-config-contrib.xml")
public class TestBlobImportKafka extends TestBlobImport {

    @BeforeClass
    public static void assumeKafkaEnabled() {
        KafkaHelper.assumeKafkaEnabled();
    }

    @Override
    public LogManager getManager() {
        KafkaConfigService service = Framework.getService(KafkaConfigService.class);
        String kafkaConfig = "default";
        return new KafkaLogManager(service.getTopicPrefix(kafkaConfig), service.getProducerProperties(kafkaConfig),
                service.getConsumerProperties(kafkaConfig));
    }
}
