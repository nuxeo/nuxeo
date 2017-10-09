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
package org.nuxeo.lib.core.mqueues.pattern.producer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.core.mqueues.mqueues.MQManager;
import org.nuxeo.lib.core.mqueues.pattern.Message;
import org.nuxeo.lib.core.mqueues.pattern.consumer.internals.AbstractCallablePool;
import org.nuxeo.lib.core.mqueues.pattern.producer.internals.ProducerRunner;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * A Pool of ProducerRunner
 *
 * @since 9.1
 */
public class ProducerPool<M extends Message> extends AbstractCallablePool<ProducerStatus> {
    private static final Log log = LogFactory.getLog(ProducerPool.class);
    protected final MQManager manager;
    protected final ProducerFactory<M> factory;
    protected final String mqName;

    public ProducerPool(String mqName, MQManager manager, ProducerFactory<M> factory, short nbThreads) {
        super(nbThreads);
        this.mqName = mqName;
        this.manager = manager;
        this.factory = factory;
    }

    @Override
    protected ProducerStatus getErrorStatus() {
        return new ProducerStatus(0, 0, 0, 0, true);
    }

    @Override
    protected Callable<ProducerStatus> getCallable(int i) {
        return new ProducerRunner<>(factory, manager.getAppender(mqName), i);
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
