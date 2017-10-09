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
package org.nuxeo.lib.core.mqueues.tests.computation;

import org.nuxeo.lib.core.mqueues.mqueues.kafka.KafkaMQManager;
import org.nuxeo.lib.core.mqueues.tests.mqueues.TestMQueueKafka;

import java.util.Properties;

/**
 * Kafka test using manual assignment of topic/partitions
 * @since 9.2
 */
public class TestMQComputationManagerKafkaNoSubscribe extends TestMQComputationManagerKafka {

    @Override
    protected Properties getConsumerProps() {
        Properties ret = (Properties) TestMQueueKafka.getConsumerProps().clone();
        ret.put(KafkaMQManager.DISABLE_SUBSCRIBE_PROP, "true");
        return ret;
    }

}
