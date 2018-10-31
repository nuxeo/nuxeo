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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Test;
import org.nuxeo.lib.stream.computation.Computation;
import org.nuxeo.lib.stream.computation.ComputationMetadataMapping;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Watermark;
import org.nuxeo.lib.stream.computation.internals.ComputationContextImpl;

/**
 * @since 9.3
 */
public class TestComputation {

    @Test
    public void testComputationForward() throws Exception {
        // create a computation with 2 inputs streams and 4 output streams
        Computation comp = new ComputationForward("foo", 2, 4);

        // check expected metadata
        assertEquals("foo", comp.metadata().name());
        assertEquals(new HashSet<>(Arrays.asList("i1", "i2")), comp.metadata().inputStreams());
        assertEquals(new HashSet<>(Arrays.asList("o1", "o2", "o3", "o4")), comp.metadata().outputStreams());

        // create a context
        ComputationContextImpl context = new ComputationContextImpl(
                new ComputationMetadataMapping(comp.metadata(), Collections.emptyMap()));

        // init the computation
        comp.init(context);

        // there is no timer
        assertEquals(0, context.getTimers().size());

        // ask to process a record
        comp.processRecord(context, "i1", Record.of("foo", "bar".getBytes(UTF_8)));

        // the record is forwarded to one output stream
        assertEquals(1, context.getRecords("o1").size());
        assertEquals(0, context.getRecords("o2").size());
        assertEquals(0, context.getRecords("o3").size());
        assertEquals(0, context.getRecords("o4").size());

        assertEquals("foo", context.getRecords("o1").get(0).getKey());
        assertEquals("bar", new String(context.getRecords("o1").get(0).getData(), UTF_8));

        // ask to process another record
        comp.processRecord(context, "i1", Record.of("foo", "bar".getBytes(UTF_8)));
        // the record is forwarded to the second output stream
        assertEquals(1, context.getRecords("o2").size());
        assertEquals(0, context.getRecords("o3").size());
        assertEquals(0, context.getRecords("o4").size());

        comp.destroy();
    }

    @Test
    public void testComputationSource() {
        int nbRecordsToGenerate = 7;
        int batchSize = 3;
        int outputStreams = 2;
        long t0 = System.currentTimeMillis();

        Computation comp = new ComputationSource("foo", outputStreams, nbRecordsToGenerate, batchSize, t0);
        assertEquals("foo", comp.metadata().name());
        assertEquals(Collections.emptySet(), comp.metadata().inputStreams());
        assertEquals(new HashSet<>(Arrays.asList("o1", "o2")), comp.metadata().outputStreams());

        ComputationContextImpl context = new ComputationContextImpl(
                new ComputationMetadataMapping(comp.metadata(), Collections.emptyMap()));
        comp.init(context);

        // there is a timer
        assertEquals(1, context.getTimers().size());
        String timerKey = (String) context.getTimers().keySet().toArray()[0];

        // execute the timer 3 times
        comp.processTimer(context, timerKey, 0);
        comp.processTimer(context, timerKey, 0);
        comp.processTimer(context, timerKey, 0);

        // this produces 10 record on each output stream
        assertEquals(nbRecordsToGenerate, context.getRecords("o1").size());
        assertEquals(nbRecordsToGenerate, context.getRecords("o2").size());
        // System.out.println(t0);
        // context.getRecords("o1").stream().forEach(record -> System.out.println(Watermark.of(record.watermark)));
        // end up with the expected timestamp
        assertEquals(t0, Watermark.ofValue(context.getSourceLowWatermark()).getTimestamp());

        comp.destroy();
    }

    @Test
    public void testComputationRecordCounter() throws Exception {
        Duration interval = Duration.ofMillis(20);
        Computation comp = new ComputationRecordCounter("foo", interval);
        assertEquals("foo", comp.metadata().name());
        assertEquals(new HashSet<>(Collections.singletonList("i1")), comp.metadata().inputStreams());
        assertEquals(new HashSet<>(Collections.singletonList("o1")), comp.metadata().outputStreams());

        ComputationContextImpl context = new ComputationContextImpl(
                new ComputationMetadataMapping(comp.metadata(), Collections.emptyMap()));
        comp.init(context);
        // a timer is set
        assertEquals(1, context.getTimers().size());

        for (int i = 0; i < 42; i++) {
            comp.processRecord(context, "i1", Record.of("foo", "bar".getBytes(UTF_8)));
        }
        // nothing on output because the output is done by the timer
        assertEquals(0, context.getRecords("o1").size());
        // call the timer
        comp.processTimer(context, "sum", 0);
        // we have a response
        assertEquals(1, context.getRecords("o1").size());

        // the key contains the total
        assertEquals("42", context.getRecords("o1").get(0).getKey());

        // Add a new record
        comp.processRecord(context, "i2", Record.of("foo", null));

        // call the timer
        comp.processTimer(context, "sum", 0);
        // we now have 2 counter results
        assertEquals(2, context.getRecords("o1").size());
        // the counter has been reset
        assertEquals("1", context.getRecords("o1").get(1).getKey());
    }

}
