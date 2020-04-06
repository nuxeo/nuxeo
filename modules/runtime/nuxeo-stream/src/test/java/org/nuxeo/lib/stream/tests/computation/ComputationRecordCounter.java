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

import java.time.Duration;

import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;

/**
 * Computation that count input records and output counter at fixed interval.
 *
 * @since 9.3
 */
public class ComputationRecordCounter extends AbstractComputation {
    protected final long intervalMs;

    protected int count;

    protected long lastWatermark;

    /**
     * Output record counter every interval.
     */
    public ComputationRecordCounter(String name, Duration interval) {
        super(name, 1, 1);
        this.intervalMs = interval.toMillis();
    }

    @Override
    public void init(ComputationContext context) {
        context.setTimer("sum", System.currentTimeMillis() + intervalMs);
        count = 0;
        lastWatermark = 0;
    }

    @Override
    public void processRecord(ComputationContext context, String inputStreamName, Record record) {
        lastWatermark = record.getWatermark();
        count += 1;
    }

    @Override
    public void processTimer(ComputationContext context, String key, long time) {
        context.setSourceLowWatermark(lastWatermark);
        context.produceRecord(OUTPUT_1, Integer.toString(count), null);
        count = 0;
        context.setTimer("sum", System.currentTimeMillis() + intervalMs);
        context.askForCheckpoint();
    }

}
