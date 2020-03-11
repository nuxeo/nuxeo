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
package org.nuxeo.runtime.stream.tests;

import java.util.concurrent.TimeUnit;

import org.nuxeo.lib.stream.computation.ComputationPolicy;
import org.nuxeo.lib.stream.computation.ComputationPolicyBuilder;
import org.nuxeo.runtime.stream.StreamComputationPolicy;
import org.nuxeo.runtime.stream.StreamProcessorDescriptor;

import net.jodah.failsafe.RetryPolicy;

/**
 * Example of a custom Computation Policy
 *
 * @since 10.3
 */
public class MyPolicy implements StreamComputationPolicy {

    @Override
    public ComputationPolicy getPolicy(StreamProcessorDescriptor.PolicyDescriptor descriptor) {
        // Here we can use the full RetryPolicy API
        RetryPolicy retryPolicy = new RetryPolicy().withMaxRetries(descriptor.maxRetries)
                                                   .withBackoff(descriptor.delay.toMillis(),
                                                           descriptor.maxDelay.toMillis(), TimeUnit.MILLISECONDS);
        retryPolicy.retryOn(IllegalStateException.class, IllegalArgumentException.class);
        ComputationPolicyBuilder builder = descriptor.createPolicyBuilder();
        return builder.retryPolicy(retryPolicy).build();
    }
}
