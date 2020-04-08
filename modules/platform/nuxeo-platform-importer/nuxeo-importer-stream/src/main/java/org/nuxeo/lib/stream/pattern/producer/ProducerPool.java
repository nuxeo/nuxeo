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
package org.nuxeo.lib.stream.pattern.producer;

import static org.nuxeo.lib.stream.codec.NoCodec.NO_CODEC;

import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.pattern.Message;
import org.nuxeo.lib.stream.pattern.consumer.internals.AbstractCallablePool;
import org.nuxeo.lib.stream.pattern.producer.internals.ProducerRunner;

/**
 * A Pool of ProducerRunner
 *
 * @since 9.1
 */
public class ProducerPool<M extends Message> extends AbstractCallablePool<ProducerStatus> {
    private static final Log log = LogFactory.getLog(ProducerPool.class);

    protected final LogManager manager;

    protected final String logName;

    protected final Codec<M> codec;

    protected final ProducerFactory<M> factory;

    /**
     * @deprecated since 11.1, due to serialization issue with java 11, use
     *             {@link #ProducerPool(String, LogManager, Codec, ProducerFactory, short)} which allows to give a
     *             {@link org.nuxeo.lib.stream.codec.Codec codec} to {@link LogAppender appender}.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public ProducerPool(String logName, LogManager manager, ProducerFactory<M> factory, short nbThreads) {
        this(logName, manager, NO_CODEC, factory, nbThreads);
    }

    public ProducerPool(String logName, LogManager manager, Codec<M> codec, ProducerFactory<M> factory,
            short nbThreads) {
        super(nbThreads);
        this.logName = logName;
        this.manager = manager;
        this.codec = codec;
        this.factory = factory;
    }

    @Override
    protected ProducerStatus getErrorStatus() {
        return new ProducerStatus(0, 0, 0, 0, true);
    }

    @Override
    protected Callable<ProducerStatus> getCallable(int i) {
        return new ProducerRunner<>(factory, manager.getAppender(Name.ofUrn(logName), codec), i);
    }

    @Override
    protected String getThreadPrefix() {
        return "Nuxeo-Producer";
    }

    @Override
    protected void afterCall(List<ProducerStatus> ret) {
        ret.forEach(log::info);
        log.warn(ProducerStatus.toString(ret));
    }

}
