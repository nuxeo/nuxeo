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
package org.nuxeo.runtime.pubsub;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.cluster.ClusterService;
import org.nuxeo.runtime.codec.CodecService;
import org.nuxeo.runtime.stream.StreamService;

/**
 * A Pub/Sub provider based on Nuxeo Stream.
 *
 * @since 10.1
 */
public class StreamPubSubProvider extends AbstractPubSubProvider {
    private static final Log log = LogFactory.getLog(StreamPubSubProvider.class);

    public static final String GROUP_PREFIX = "pubsub/pubSub-";

    protected static final String LOG_CONFIG_OPT = "logConfig";

    protected static final String DEFAULT_LOG_CONFIG = "default";

    protected static final String LOG_NAME_OPT = "logName";

    protected static final String CODEC_OPT = "codec";

    protected static final String DEFAULT_CODEC = "avroBinary";

    protected static final Random RANDOM = new Random(); // NOSONAR (doesn't need cryptographic strength)

    protected String logConfig;

    protected Name logName;

    protected LogAppender<Record> appender;

    protected Thread thread;

    protected Codec<Record> codec;

    protected String nodeId;

    @Override
    public void initialize(Map<String, String> options, Map<String, List<BiConsumer<String, byte[]>>> subscribers) {
        log.debug("Initializing ");
        super.initialize(options, subscribers);
        logConfig = options.getOrDefault(LOG_CONFIG_OPT, DEFAULT_LOG_CONFIG);
        logName = Name.ofUrn(options.get(LOG_NAME_OPT));
        String codecName = options.getOrDefault(CODEC_OPT, DEFAULT_CODEC);
        CodecService codecService = Framework.getService(CodecService.class);
        codec = codecService.getCodec(codecName, Record.class);
        appender = Framework.getService(StreamService.class).getLogManager(logConfig).getAppender(logName, codec);
        nodeId = Framework.getService(ClusterService.class).getNodeId();
        startConsumerThread();
        log.debug("Initialized");
    }

    protected void startConsumerThread() {
        Subscriber subscriber = new Subscriber();
        thread = new Thread(subscriber, "Nuxeo-PubSub-Stream");
        thread.setUncaughtExceptionHandler((t, e) -> log.error("Uncaught error on thread " + t.getName(), e));
        thread.setPriority(Thread.NORM_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void publish(String topic, byte[] message) {
        appender.append(topic, Record.of(topic, message));
    }

    @Override
    public void close() {
        appender = null;
        if (thread != null) {
            thread.interrupt();
            thread = null;
            log.debug("Closed");
        }
    }

    public class Subscriber implements Runnable {

        @Override
        public void run() {
            // using different group name enable fan out
            Name group = Name.ofUrn(GROUP_PREFIX + nodeId);
            log.debug("Starting subscriber thread with group: " + group);
            try (LogTailer<Record> tailer = Framework.getService(StreamService.class)
                                                     .getLogManager(logConfig)
                                                     .createTailer(group, logName, codec)) {
                // Only interested in new messages
                tailer.toEnd();
                for (;;) {
                    try {
                        LogRecord<Record> logRecord = tailer.read(Duration.ofSeconds(5));
                        if (logRecord == null) {
                            continue;
                        }
                        Record record = logRecord.message();
                        localPublish(record.getKey(), record.getData());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.debug("Subscriber thread interrupted, exiting");
                        return;
                    }
                }
            }

        }
    }

}
