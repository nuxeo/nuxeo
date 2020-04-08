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

import java.time.Duration;

import net.jodah.failsafe.RetryPolicy;

/**
 * @since 9.2
 */
public class ConsumerPolicyBuilder {
    protected BatchPolicy batchPolicy = BatchPolicy.DEFAULT;

    protected RetryPolicy retryPolicy = ConsumerPolicy.NO_RETRY;

    protected boolean skipFailure = false;

    protected Duration waitMessageTimeout = Duration.ofSeconds(2);

    protected ConsumerPolicy.StartOffset startOffset = ConsumerPolicy.StartOffset.LAST_COMMITTED;

    protected boolean salted = false;

    protected String name;

    protected short maxThreads = 0;

    protected ConsumerPolicyBuilder() {

    }

    public ConsumerPolicyBuilder batchPolicy(BatchPolicy policy) {
        batchPolicy = policy;
        return this;
    }

    public ConsumerPolicyBuilder retryPolicy(RetryPolicy policy) {
        retryPolicy = policy;
        return this;
    }

    /**
     * Continue on next message even if the retry policy has failed.
     */
    public ConsumerPolicyBuilder continueOnFailure(boolean value) {
        skipFailure = value;
        return this;
    }

    /**
     * Maximum consumer threads to use. The number of threads is limited by the size of the Log.
     */
    public ConsumerPolicyBuilder maxThreads(short maxThreads) {
        this.maxThreads = maxThreads;
        return this;
    }

    /**
     * Consumer will stop if there is no more message after this timeout.
     */
    public ConsumerPolicyBuilder waitMessageTimeout(Duration duration) {
        waitMessageTimeout = duration;
        return this;
    }

    /**
     * Consumer will wait for ever message.
     */
    public ConsumerPolicyBuilder waitMessageForEver() {
        waitMessageTimeout = Duration.ofSeconds(Integer.MAX_VALUE);
        return this;
    }

    /**
     * Where to read the first message.
     */
    public ConsumerPolicyBuilder startOffset(ConsumerPolicy.StartOffset startOffset) {
        this.startOffset = startOffset;
        return this;
    }

    /**
     * Consumer will wait some random time before start, to prevent wave of concurrency in batch processing.
     */
    public ConsumerPolicyBuilder salted() {
        salted = true;
        return this;
    }

    /**
     * Consumer group name.
     */
    public ConsumerPolicyBuilder name(String name) {
        this.name = name.replace(".", "/");
        return this;
    }

    public ConsumerPolicy build() {
        return new ConsumerPolicy(this);
    }

}
