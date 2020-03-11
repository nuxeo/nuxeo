/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.stream;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.nuxeo.lib.stream.computation.ComputationPolicy;
import org.nuxeo.lib.stream.computation.ComputationPolicyBuilder;

import net.jodah.failsafe.RetryPolicy;

/**
 * A Computation policy that can be use for Nuxeo.
 *
 * @since 11.1
 */
public class DefaultNuxeoComputationPolicy implements StreamComputationPolicy {

    @Override
    public ComputationPolicy getPolicy(StreamProcessorDescriptor.PolicyDescriptor descriptor) {
        RetryPolicy retryPolicy = new RetryPolicy().withMaxRetries(descriptor.maxRetries)
                                                   .withBackoff(descriptor.delay.toMillis(),
                                                           descriptor.maxDelay.toMillis(), TimeUnit.MILLISECONDS);
        // NuxeoException and ConcurrentUpdateException are assignable from RuntimeException
        retryPolicy.retryOn(RuntimeException.class, TimeoutException.class, IOException.class, SQLException.class);
        ComputationPolicyBuilder builder = descriptor.createPolicyBuilder();
        builder.retryPolicy(retryPolicy);
        return builder.build();
    }
}
