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
import org.nuxeo.lib.stream.computation.Record;

/**
 * Same as {@link ComputationForward} but add latency on processing.
 *
 * @since 9.3
 */
public class ComputationForwardSlow extends ComputationForward {
    protected final int averageDelayMs;

    public ComputationForwardSlow(String name, int inputs, int outputs) {
        this(name, inputs, outputs, 10);
    }

    public ComputationForwardSlow(String name, int inputs, int outputs, int averageDelayMs) {
        super(name, inputs, outputs);
        this.averageDelayMs = averageDelayMs;
    }

    @SuppressWarnings("squid:S2925")
    @Override
    public void processRecord(ComputationContext context, String inputStreamName, Record record) {
        try {
            Thread.sleep(averageDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        super.processRecord(context, inputStreamName, record);
    }
}
