/*
 * (C) Copyright 2017-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     bdelbosc
 */
package org.nuxeo.runtime.stream;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.lib.stream.StreamRuntimeException;
import org.nuxeo.lib.stream.computation.ComputationPolicy;
import org.nuxeo.lib.stream.computation.ComputationPolicyBuilder;
import org.nuxeo.runtime.model.Descriptor;

import net.jodah.failsafe.RetryPolicy;

@XObject("streamProcessor")
public class StreamProcessorDescriptor implements Descriptor {

    @XObject(value = "computation")
    public static class ComputationDescriptor implements Descriptor {

        @XNode("@name")
        public String name;

        @XNode("@concurrency")
        public Integer concurrency = DEFAULT_CONCURRENCY;

        @Override
        public String getId() {
            return name;
        }
    }

    @XObject(value = "stream")
    public static class StreamDescriptor implements Descriptor {

        @XNode("@name")
        public String name;

        @XNode("@partitions")
        public Integer partitions = DEFAULT_CONCURRENCY;

        @XNode("@codec")
        public String codec;

        @Override
        public String getId() {
            return name;
        }

    }

    @XObject(value = "policy")
    public static class PolicyDescriptor implements Descriptor {
        public static final int DEFAULT_MAX_RETRIES = 0;

        public static final Duration DEFAULT_DELAY = Duration.ofSeconds(1);

        public static final Duration DEFAULT_MAX_DELAY = Duration.ofSeconds(10);

        public static final Integer DEFAULT_BATCH_CAPACITY = 1;

        public static final Duration DEFAULT_BATCH_THRESHOLD = Duration.ofSeconds(1);

        @XNode("@name")
        public String name;

        @XNode("@maxRetries")
        public Integer maxRetries = DEFAULT_MAX_RETRIES;

        @XNode("@delay")
        public Duration delay = DEFAULT_DELAY;

        @XNode("@maxDelay")
        public Duration maxDelay = DEFAULT_MAX_DELAY;

        @XNode("@continueOnFailure")
        public Boolean continueOnFailure = Boolean.FALSE;

        @Override
        public String getId() {
            return name;
        }

        // To provide a custom retry policy
        @XNode("@class")
        public Class<? extends StreamComputationPolicy> klass;

        // Batch policy only used for computation that extends AbstractBatchComputation
        @XNode("@batchCapacity")
        public Integer batchCapacity = DEFAULT_BATCH_CAPACITY;

        @XNode("@batchThreshold")
        public Duration batchThreshold = DEFAULT_BATCH_THRESHOLD;

    }

    public static final Integer DEFAULT_CONCURRENCY = 4;

    @XNode("@name")
    public String name;

    @XNode("@logConfig")
    public String config;

    @XNode("@class")
    public Class<? extends StreamProcessorTopology> klass;

    @XNode("@defaultConcurrency")
    public Integer defaultConcurrency = DEFAULT_CONCURRENCY;

    @XNode("@defaultPartitions")
    public Integer defaultPartitions = DEFAULT_CONCURRENCY;

    @XNode("@defaultCodec")
    public String defaultCodec;

    @XNodeMap(value = "option", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> options = new HashMap<>();

    @XNodeList(value = "computation", type = ArrayList.class, componentType = ComputationDescriptor.class)
    public List<ComputationDescriptor> computations = new ArrayList<>();

    @XNodeList(value = "stream", type = ArrayList.class, componentType = StreamDescriptor.class)
    public List<StreamDescriptor> streams = new ArrayList<>();

    @XNodeList(value = "policy", type = ArrayList.class, componentType = PolicyDescriptor.class)
    public List<PolicyDescriptor> policies = new ArrayList<>();

    protected ComputationPolicy defaultPolicy;

    public ComputationPolicy getPolicy(String computationName) {
        PolicyDescriptor policyDescriptor = policies.stream()
                                                    .filter(policy -> computationName.equals(policy.getId()))
                                                    .findFirst()
                                                    .orElse(null);
        if (policyDescriptor != null) {
            return getComputationPolicy(policyDescriptor);
        }
        return null;
    }

    protected ComputationPolicy getComputationPolicy(PolicyDescriptor policyDescriptor) {
        if (policyDescriptor.klass != null) {
            if (!StreamComputationPolicy.class.isAssignableFrom(policyDescriptor.klass)) {
                throw new IllegalArgumentException("Cannot create policy: " + policyDescriptor.getId()
                        + " for processor: " + this.getId() + ", class must implement StreamComputationPolicy");
            }
            try {
                return policyDescriptor.klass.getDeclaredConstructor().newInstance().getPolicy(policyDescriptor);
            } catch (ReflectiveOperationException e) {
                throw new StreamRuntimeException(
                        "Cannot create policy: " + policyDescriptor.getId() + " for processor: " + this.getId(), e);
            }
        }
        RetryPolicy retryPolicy = new RetryPolicy().withMaxRetries(policyDescriptor.maxRetries)
                                                   .withBackoff(policyDescriptor.delay.toMillis(),
                                                           policyDescriptor.maxDelay.toMillis(), TimeUnit.MILLISECONDS);
        return new ComputationPolicyBuilder().retryPolicy(retryPolicy)
                                             .batchPolicy(policyDescriptor.batchCapacity,
                                                     policyDescriptor.batchThreshold)
                                             .continueOnFailure(policyDescriptor.continueOnFailure)
                                             .build();
    }

    public ComputationPolicy getDefaultPolicy() {
        if (defaultPolicy == null) {
            PolicyDescriptor policyDescriptor = policies.stream()
                                                        .filter(policy -> "default".equals(policy.getId()))
                                                        .findFirst()
                                                        .orElse(null);
            if (policyDescriptor == null) {
                defaultPolicy = ComputationPolicy.NONE;
            } else {
                defaultPolicy = getComputationPolicy(policyDescriptor);
            }
        }
        return defaultPolicy;
    }

    @Override
    public String getId() {
        return name;
    }

}
