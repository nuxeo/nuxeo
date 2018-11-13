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
package org.nuxeo.lib.stream.tests.computation;

import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.ComputationPolicy;
import org.nuxeo.lib.stream.computation.Record;

/**
 * Computation that fails on the 3 first records and timer
 *
 * @since 10.3
 */
public class ComputationFailureForward extends ComputationForward {
    public static final int FAILURE_COUNT = 3;

    protected int processCounter;

    public ComputationFailureForward(String name, int inputs, int outputs, ComputationPolicy policy) {
        super(name, inputs, outputs);
        setPolicy(policy);
    }

    public ComputationFailureForward(String name, int inputs, int outputs) {
        super(name, inputs, outputs);
    }

    @Override
    public void processTimer(ComputationContext context, String key, long timestamp) {
        super.processTimer(context, key, timestamp);
        if (processCounter++ < FAILURE_COUNT) {
            throw new IllegalStateException("Simulated error for test purpose");
        }
    }

    @Override
    public void processRecord(ComputationContext context, String inputStreamName, Record record) {
        super.processRecord(context, inputStreamName, record);
        if (processCounter++ < FAILURE_COUNT) {
            throw new IllegalStateException("Simulated error for test purpose");
        }
    }
}
