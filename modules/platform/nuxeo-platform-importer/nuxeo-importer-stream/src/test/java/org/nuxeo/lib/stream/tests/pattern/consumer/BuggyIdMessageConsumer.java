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
package org.nuxeo.lib.stream.tests.pattern.consumer;

import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.pattern.KeyValueMessage;
import org.nuxeo.lib.stream.pattern.consumer.AbstractConsumer;

/**
 * @since 9.1
 */
public class BuggyIdMessageConsumer extends AbstractConsumer<KeyValueMessage> {
    private static final Log log = LogFactory.getLog(BuggyIdMessageConsumer.class);

    protected long lastAccepted = -1;

    protected long lastCommitted = -1;

    public BuggyIdMessageConsumer(String consumerId) {
        super(consumerId);
    }

    @Override
    public void begin() {
        if (getRandom100() < 1) {
            throw new BuggyException("Failure in begin");
        }
        log.trace("begin " + getConsumerId());

    }

    @Override
    public void accept(KeyValueMessage message) {
        if (getRandom100() < 10) {
            throw new BuggyException("Failure in accept: " + message);
        }

        long tmp = Long.valueOf(message.getId());
        if (log.isTraceEnabled()) {
            log.trace(getConsumerId() + " accept: " + tmp);
        }
        // these assertions are not valid when tailing from multiple partitions

        // // ensure that message are always bigger than the last committed one
        // if (lastCommitted >= 0 && tmp <= lastCommitted) {
        // String msg = "Error receive unordered message: " + tmp + " < last committed: " + lastCommitted;
        // log.error(msg);
        // throw new IllegalStateException(msg);
        // }
        // // ensure that message arrive ordered
        // if (lastAccepted >= 0 && tmp <= lastAccepted) {
        // String msg = "Error receive unordered message: " + tmp + " < last accepted : " + lastAccepted;
        // log.error(msg);
        // throw new IllegalStateException(msg);
        // }

        lastAccepted = tmp;
    }

    @Override
    public void commit() {
        if (getRandom100() < 2) {
            throw new BuggyException("Failure in commit");
        }
        lastCommitted = lastAccepted;
        if (log.isTraceEnabled()) {
            log.trace(getConsumerId() + " commit " + lastCommitted);
        }

    }

    @Override
    public void rollback() {
        if (getRandom100() < 0) {
            throw new BuggyException("Failure in rollback");
        }
        lastAccepted = lastCommitted;
        if (log.isTraceEnabled()) {
            log.trace(getConsumerId() + " rollback to " + lastCommitted);
        }
    }

    protected int getRandom100() {
        return ThreadLocalRandom.current().nextInt(100);
    }

}
