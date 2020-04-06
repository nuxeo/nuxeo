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
package org.nuxeo.lib.stream.tests.computation;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;

import org.junit.Test;
import org.nuxeo.lib.stream.computation.AbstractBatchComputation;
import org.nuxeo.lib.stream.computation.ComputationMetadataMapping;
import org.nuxeo.lib.stream.computation.ComputationPolicy;
import org.nuxeo.lib.stream.computation.ComputationPolicyBuilder;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.internals.ComputationContextImpl;

/**
 * @since 9.3
 */
public class TestBatchComputation {

    @Test
    public void testComputationBatchForward() {
        int batchCapacity = 5;
        ComputationPolicy policy = new ComputationPolicyBuilder().batchPolicy(batchCapacity, Duration.ofMillis(500))
                                                                 .build();
        ComputationBatchForward comp = new ComputationBatchForward("foo", 2);

        // create a context
        ComputationContextImpl context = new ComputationContextImpl(null,
                new ComputationMetadataMapping(comp.metadata(), Collections.emptyMap()), policy);

        // init the computation
        comp.init(context);

        // there is a batch timer
        assertEquals(1, context.getTimers().size());

        // assertion counter start
        assertEquals(0, comp.failureCounter);
        assertEquals(0, comp.processCounter);

        // send a record
        Record aRecord = Record.of("foo", "bar".getBytes(StandardCharsets.UTF_8));
        comp.processRecord(context, "i1", aRecord);
        // no record send to output
        assertEquals(0, context.getRecords(OUTPUT_1).size());
        // assertion counter start
        assertEquals(0, comp.failureCounter);
        assertEquals(0, comp.processCounter);

        // check batch on capacity
        for (int i = 0; i < batchCapacity; i++) {
            comp.processRecord(context, "i1", aRecord);
        }
        // one batch sent
        assertEquals(batchCapacity, context.getRecords("o1").size());
        context.getRecords("o1").clear();
        // assertion counter start
        assertEquals(0, comp.failureCounter);
        assertEquals(1, comp.processCounter);

        // check batch on timer threshold
        // there is a one record pending, so triggering the timer should send it
        comp.processTimer(context, AbstractBatchComputation.TIMER_BATCH, 0);
        assertEquals(0, comp.failureCounter);
        assertEquals(2, comp.processCounter);
        assertEquals(1, context.getRecords("o1").size());
        context.getRecords("o1").clear();

        // check batch done when changing input stream
        comp.processRecord(context, "i1", aRecord);
        assertEquals(0, comp.failureCounter);
        assertEquals(2, comp.processCounter);
        assertEquals(0, context.getRecords(OUTPUT_1).size());

        comp.processRecord(context, "i2", aRecord);
        assertEquals(1, context.getRecords("o1").size());
        assertEquals(0, comp.failureCounter);
        assertEquals(3, comp.processCounter);

        comp.destroy();
    }

}
