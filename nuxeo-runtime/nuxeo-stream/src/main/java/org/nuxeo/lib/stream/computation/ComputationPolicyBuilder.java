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
 * Builder to create a ComputationPolicy.
 *
 * @since 10.3
 */
public class ComputationPolicyBuilder {

    protected static final int DEFAULT_BATCH_CAPACITY = 10;

    protected static final int DEFAULT_BATCH_THRESHOLD_SECOND = 1;

    protected RetryPolicy retryPolicy = ComputationPolicy.NO_RETRY;

    protected boolean skipFailure = false;

    protected int batchCapacity = DEFAULT_BATCH_CAPACITY;

    protected Duration batchThreshold = Duration.ofSeconds(DEFAULT_BATCH_THRESHOLD_SECOND);

    public ComputationPolicyBuilder() {
        // Empty constructor
    }

    /**
     * Defines how to group records by batch using a capacity and a time threshold.
     *
     * @param capacity the number of records in the batch
     * @param timeThreshold process the batch even if not full after this duration
     */
    public ComputationPolicyBuilder batchPolicy(int capacity, Duration timeThreshold) {
        batchCapacity = capacity;
        batchThreshold = timeThreshold;
        return this;
    }

    /**
     * Defines what to do in case of failure during the batch processing.
     */
    public ComputationPolicyBuilder retryPolicy(RetryPolicy policy) {
        retryPolicy = policy;
        return this;
    }

    /**
     * The fallback when processing a batch has failed after applying the retry policy has failed.
     *
     * @param value When {@code true} Skips the records affected by the batch in failure and continue.<br/>
     *            When {@code false} aborts the computation, this is the default behavior.
     */
    public ComputationPolicyBuilder continueOnFailure(boolean value) {
        skipFailure = value;
        return this;
    }

    /**
     * Creates the policy.
     */
    public ComputationPolicy build() {
        return new ComputationPolicy(this);
    }

}
