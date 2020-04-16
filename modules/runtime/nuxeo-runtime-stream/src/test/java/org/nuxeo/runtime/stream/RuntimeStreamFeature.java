/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.runtime.stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

/**
 * The runtime stream feature provides a Chronicle or Kafka stream implementation depending on test configuration.
 * <p>
 * To run your unit tests on a Chronicle or Kafka you need to declare {@code nuxeo.test.stream} to either
 * {@link #STREAM_CHRONICLE chronicle} or {@link #STREAM_KAFKA kafka} in your system properties.
 * <p>
 * Once your tests use this feature, you can deploy new {@code logConfig} like below:
 * <pre>
 * {@code <extension target="org.nuxeo.runtime.stream.service" point="logConfig">
 *     <logConfig name="MY_LOG_CONFIG_NAME" type="${nuxeo.test.stream}" />
 *   </extension>
 * }
 * </pre>
 *
 * @since 10.3
 */
@Deploy("org.nuxeo.runtime.stream")
@Deploy("org.nuxeo.runtime.stream.test")
@Features(RuntimeFeature.class)
public class RuntimeStreamFeature implements RunnerFeature {

    private static final Log log = LogFactory.getLog(RuntimeStreamFeature.class);

    public static final String BUNDLE_TEST_NAME = "org.nuxeo.runtime.stream.test";

    public static final String STREAM_PROPERTY = "nuxeo.test.stream";

    public static final String STREAM_CHRONICLE = "chronicle";

    public static final String STREAM_KAFKA = "kafka";

    // kafka properties part

    public static final String KAFKA_SERVERS_PROPERTY = "nuxeo.test.kafka.servers";

    public static final String KAFKA_SERVERS_DEFAULT = "localhost:9092";

    protected String streamType;

    protected static String defaultProperty(String name, String def) {
        String value = System.getProperty(name);
        if (value == null || value.isEmpty() || value.equals("${" + name + "}")) {
            value = def;
        }
        Framework.getProperties().setProperty(name, value);
        return value;
    }

    @Override
    public void start(FeaturesRunner runner) {
        RuntimeHarness harness = runner.getFeature(RuntimeFeature.class).getHarness();
        streamType = defaultProperty(STREAM_PROPERTY, STREAM_CHRONICLE);
        try {
            String msg = "Deploying Nuxeo Stream using " + StringUtils.capitalize(streamType.toLowerCase());
            // System.out used on purpose, don't remove
            System.out.println(getClass().getSimpleName() + ": " + msg); // NOSONAR
            log.info(msg);
            switch (streamType) {
            case STREAM_CHRONICLE:
                initChronicle(harness);
                break;
            case STREAM_KAFKA:
                initKafka(harness);
                break;
            default:
                throw new UnsupportedOperationException(streamType + " stream type is not supported");
            }
        } catch (Exception e) {
            throw new RuntimeServiceException("Unable to configure the stream implementation", e);
        }
    }

    protected void initChronicle(RuntimeHarness harness) throws Exception {
        harness.deployContrib(BUNDLE_TEST_NAME, "OSGI-INF/test-stream-chronicle-contrib.xml");
    }

    protected void initKafka(RuntimeHarness harness) throws Exception {
        // no need to re-init kafka as we use a random prefix
        defaultProperty(KAFKA_SERVERS_PROPERTY, KAFKA_SERVERS_DEFAULT);
        // deploy component
        harness.deployContrib(BUNDLE_TEST_NAME, "OSGI-INF/test-stream-kafka-contrib.xml");
    }

}
