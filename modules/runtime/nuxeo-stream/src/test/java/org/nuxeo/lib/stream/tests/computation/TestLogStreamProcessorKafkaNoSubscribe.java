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

import java.util.Properties;

import org.nuxeo.lib.stream.log.kafka.KafkaLogManager;
import org.nuxeo.lib.stream.tests.log.TestLogKafka;

/**
 * Kafka test using manual assignment of topic/partitions
 *
 * @since 9.3
 */
public class TestLogStreamProcessorKafkaNoSubscribe extends TestLogStreamProcessorKafka {

    @Override
    protected Properties getConsumerProps() {
        Properties ret = (Properties) TestLogKafka.getConsumerProps().clone();
        ret.put(KafkaLogManager.DISABLE_SUBSCRIBE_PROP, "true");
        return ret;
    }

}
