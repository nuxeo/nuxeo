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

/**
 * Describe when a batch must be flushed.
 *
 * @since 9.1
 */
public class BatchPolicy {
    public static final BatchPolicy NO_BATCH = builder().capacity(1).build();

    public static final BatchPolicy DEFAULT = builder().build();

    protected final int capacity;

    protected final Duration threshold;

    public BatchPolicy(Builder builder) {
        capacity = builder.capacity;
        threshold = builder.threshold;
    }

    public int getCapacity() {
        return capacity;
    }

    public Duration getTimeThreshold() {
        return threshold;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        protected int capacity = 10;

        protected Duration threshold = Duration.ofSeconds(10);

        protected Builder() {

        }

        /**
         * Set the capacity of the batch.
         */
        public Builder capacity(int capacity) {
            this.capacity = capacity;
            return this;
        }

        /**
         * Set the time threshold to fill a batch.
         */
        public Builder timeThreshold(Duration threshold) {
            this.threshold = threshold;
            return this;
        }

        public BatchPolicy build() {
            return new BatchPolicy(this);
        }

    }

    @Override
    public String toString() {
        return "BatchPolicy{" + "capacity=" + capacity + ", threshold=" + threshold + '}';
    }
}
