/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.pattern.KeyValueMessage;
import org.nuxeo.lib.stream.pattern.consumer.AbstractConsumer;

/**
 * The bad consumer.
 *
 * @since 10.3
 */
public class ErrorIdMessageConsumer extends AbstractConsumer<KeyValueMessage> {
    private static final Log log = LogFactory.getLog(ErrorIdMessageConsumer.class);

    public ErrorIdMessageConsumer(String consumerId) {
        super(consumerId);
    }

    @Override
    public void begin() {
        throw new Error("Simulated Error");
    }

    @Override
    public void accept(KeyValueMessage message) {
        throw new Error("Simulated Error");
    }

    @Override
    public void commit() {
        throw new Error("Simulated Error");
    }

    @Override
    public void rollback() {
        throw new Error("Simulated Error");
    }

}
