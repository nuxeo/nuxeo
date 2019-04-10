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

import org.nuxeo.lib.stream.pattern.KeyValueMessage;
import org.nuxeo.lib.stream.pattern.consumer.Consumer;
import org.nuxeo.lib.stream.pattern.consumer.ConsumerFactory;

/**
 * @since 9.1
 */
public class IdMessageFactory implements ConsumerFactory<KeyValueMessage> {
    /**
     * Factory for consumer that do nothing no op
     */
    public static final IdMessageFactory NOOP = new IdMessageFactory(ConsumerType.NOOP);

    /**
     * Factory for consumer that raise error randomly
     */
    public static final IdMessageFactory BUGGY = new IdMessageFactory(ConsumerType.BUGGY);

    /**
     * Factory for consumer that raise error randomly
     */
    public static final IdMessageFactory ERROR = new IdMessageFactory(ConsumerType.ERROR);

    protected enum ConsumerType {
        NOOP, BUGGY, ERROR
    }

    protected final ConsumerType type;

    protected IdMessageFactory(ConsumerType type) {
        this.type = type;
    }

    @Override
    public Consumer<KeyValueMessage> createConsumer(String consumerId) {
        switch (type) {
        case BUGGY:
            return new BuggyIdMessageConsumer(consumerId);
        case ERROR:
            return new ErrorIdMessageConsumer(consumerId);
        default:
        case NOOP:
            return new NoopIdMessageConsumer(consumerId);
        }
    }
}
