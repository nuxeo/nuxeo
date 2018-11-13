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
package org.nuxeo.lib.stream.computation;

import java.time.Duration;

import net.jodah.failsafe.RetryPolicy;

/**
 * Defines how a computation should handle the retries, fallback and batching if any.
 *
 * @since 10.3
 */
public class ComputationPolicy {

    public static final RetryPolicy NO_RETRY = new RetryPolicy().withMaxRetries(0);

    /* No retry, abort on failure, no batching */
    public static final ComputationPolicy NONE = new ComputationPolicyBuilder().retryPolicy(NO_RETRY)
                                                                               .continueOnFailure(false)
                                                                               .build();

    protected final RetryPolicy retryPolicy;

    protected final int batchCapacity;

    protected final Duration batchThreshold;

    protected final boolean skipFailure;

    public ComputationPolicy(ComputationPolicyBuilder builder) {
        batchCapacity = builder.batchCapacity;
        batchThreshold = builder.batchThreshold;
        skipFailure = builder.skipFailure;
        retryPolicy = builder.retryPolicy;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public int getBatchCapacity() {
        return batchCapacity;
    }

    public Duration getBatchThreshold() {
        return batchThreshold;
    }

    public boolean isSkipFailure() {
        return skipFailure;
    }

    @Override
    public String toString() {
        return "ComputationPolicy{" + "retryMax=" + retryPolicy.getMaxRetries() + ", retryDelay="
                + retryPolicy.getDelay() + ", batchCapacity=" + batchCapacity + ", batchThreshold=" + batchThreshold
                + ", skipFailure=" + skipFailure + '}';
    }
}
