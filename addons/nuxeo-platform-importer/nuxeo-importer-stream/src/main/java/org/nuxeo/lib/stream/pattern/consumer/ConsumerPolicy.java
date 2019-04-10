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
 * The consumer policy drive the consumer pool and runner.
 *
 * @since 9.1
 */
public class ConsumerPolicy {
    public static final String DEFAULT_NAME = "default";

    public enum StartOffset {
        BEGIN, END, LAST_COMMITTED
    }

    public static final RetryPolicy NO_RETRY = new RetryPolicy().withMaxRetries(0);

    /**
     * Consumer policy that stop on starvation and failure.
     */
    public static final ConsumerPolicy BOUNDED = builder().waitMessageTimeout(Duration.ofSeconds(5))
                                                          .continueOnFailure(false)
                                                          .build();

    public static final ConsumerPolicy BOUNDED_RETRY = builder().waitMessageTimeout(Duration.ofSeconds(5))
                                                                .retryPolicy(new RetryPolicy().withMaxRetries(3))
                                                                .continueOnFailure(false)
                                                                .build();

    /**
     * Consumer policy that wait for ever for new message and skip failure.
     */
    public static final ConsumerPolicy UNBOUNDED = builder().continueOnFailure(true).waitMessageForEver().build();

    public static final ConsumerPolicy UNBOUNDED_RETRY = builder().continueOnFailure(true)
                                                                  .retryPolicy(new RetryPolicy().withMaxRetries(3))
                                                                  .waitMessageForEver()
                                                                  .build();

    protected final BatchPolicy batchPolicy;

    protected final RetryPolicy retryPolicy;

    protected final boolean skipFailure;

    protected final Duration waitMessageTimeout;

    protected final StartOffset startOffset;

    protected final boolean salted;

    protected final String name;

    protected final short maxThreads;

    public ConsumerPolicy(ConsumerPolicyBuilder builder) {
        batchPolicy = builder.batchPolicy;
        retryPolicy = builder.retryPolicy;
        skipFailure = builder.skipFailure;
        waitMessageTimeout = builder.waitMessageTimeout;
        startOffset = builder.startOffset;
        salted = builder.salted;
        maxThreads = builder.maxThreads;
        if (builder.name != null) {
            name = builder.name;
        } else {
            name = DEFAULT_NAME;
        }
    }

    public BatchPolicy getBatchPolicy() {
        return batchPolicy;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public boolean continueOnFailure() {
        return skipFailure;
    }

    public Duration getWaitMessageTimeout() {
        return waitMessageTimeout;
    }

    public StartOffset getStartOffset() {
        return startOffset;
    }

    public boolean isSalted() {
        return salted;
    }

    public String getName() {
        return name;
    }

    public short getMaxThreads() {
        return maxThreads;
    }

    public static ConsumerPolicyBuilder builder() {
        return new ConsumerPolicyBuilder();
    }

    @Override
    public String toString() {
        return "ConsumerPolicy{" + "batchPolicy=" + batchPolicy + ", retryPolicy=" + retryPolicy + ", skipFailure="
                + skipFailure + ", waitMessageTimeout=" + waitMessageTimeout + ", startOffset=" + startOffset
                + ", salted=" + salted + ", name='" + name + '\'' + ", maxThreads=" + maxThreads + '}';
    }

}
