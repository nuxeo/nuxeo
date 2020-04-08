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
package org.nuxeo.lib.stream.pattern.consumer;

import static org.nuxeo.lib.stream.codec.NoCodec.NO_CODEC;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.log.kafka.KafkaUtils;
import org.nuxeo.lib.stream.pattern.Message;
import org.nuxeo.lib.stream.pattern.consumer.internals.AbstractCallablePool;
import org.nuxeo.lib.stream.pattern.consumer.internals.ConsumerRunner;

/**
 * Run a pool of ConsumerRunner.
 *
 * @since 9.1
 */
public class ConsumerPool<M extends Message> extends AbstractCallablePool<ConsumerStatus> {
    private static final Log log = LogFactory.getLog(ConsumerPool.class);

    protected final String logName;

    protected final LogManager manager;

    protected final Codec<M> codec;

    protected final ConsumerFactory<M> factory;

    protected final ConsumerPolicy policy;

    protected final List<List<LogPartition>> defaultAssignments;

    /**
     * @deprecated since 11.1, due to serialization issue with java 11, use
     *             {@link #ConsumerPool(String, LogManager, Codec, ConsumerFactory, ConsumerPolicy)} which allows to
     *             give a {@link org.nuxeo.lib.stream.codec.Codec codec} to {@link org.nuxeo.lib.stream.log.LogTailer
     *             tailer}.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public ConsumerPool(String logName, LogManager manager, ConsumerFactory<M> factory, ConsumerPolicy policy) {
        this(logName, manager, NO_CODEC, factory, policy);
    }

    public ConsumerPool(String logName, LogManager manager, Codec<M> codec, ConsumerFactory<M> factory,
            ConsumerPolicy policy) {
        super(computeNbThreads((short) manager.getAppender(Name.ofUrn(logName)).size(), policy.getMaxThreads()));
        this.logName = logName;
        this.manager = manager;
        this.codec = codec;
        this.factory = factory;
        this.policy = policy;
        this.defaultAssignments = getDefaultAssignments();
        if (manager.supportSubscribe()) {
            log.info("Creating consumer pool using Log subscribe on " + logName);
        } else {
            log.info("Creating consumer pool using Log assignments on " + logName + ": " + defaultAssignments);
        }
    }

    protected static short computeNbThreads(short maxConcurrency, short maxThreads) {
        if (maxThreads > 0) {
            return (short) Math.min(maxConcurrency, maxThreads);
        }
        return maxConcurrency;
    }

    public Name getConsumerGroupName() {
        return Name.ofUrn(policy.getName());
    }

    @Override
    protected ConsumerStatus getErrorStatus() {
        return new ConsumerStatus("error", 0, 0, 0, 0, 0, 0, true);
    }

    @Override
    protected Callable<ConsumerStatus> getCallable(int i) {
        return new ConsumerRunner<>(factory, policy, manager, codec, defaultAssignments.get(i));
    }

    @Override
    protected String getThreadPrefix() {
        return "Nuxeo-Consumer";
    }

    @Override
    protected void afterCall(List<ConsumerStatus> ret) {
        ret.forEach(log::info);
        log.warn(ConsumerStatus.toString(ret));
    }

    protected List<List<LogPartition>> getDefaultAssignments() {
        Map<String, Integer> streams = Collections.singletonMap(logName,
                manager.getAppender(Name.ofUrn(logName)).size());
        return KafkaUtils.roundRobinAssignments(getNbThreads(), streams);
    }

}
