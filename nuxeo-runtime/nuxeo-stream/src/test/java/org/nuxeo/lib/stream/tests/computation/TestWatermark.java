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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Test;
import org.nuxeo.lib.stream.computation.Watermark;
import org.nuxeo.lib.stream.computation.internals.WatermarkMonotonicInterval;

/**
 * @since 9.3
 */
public class TestWatermark {

    @Test
    public void testWmInit() {
        long t0 = System.currentTimeMillis();
        Watermark wm = Watermark.ofTimestamp(t0);
        Watermark wmBis = Watermark.ofValue(wm.getValue());
        assertEquals(wm, wmBis);
        assertEquals(t0, wmBis.getTimestamp());

        // same with a sequence
        wm = Watermark.ofTimestamp(t0, (short) 1024);
        wmBis = Watermark.ofValue(wm.getValue());
        assertEquals(wm, wmBis);
        assertEquals(t0, wmBis.getTimestamp());
        assertEquals(1024, wmBis.getSequence());
    }

    @Test
    public void testWmLimits() {

        Watermark wm = Watermark.ofTimestamp(0);
        Watermark wm2 = Watermark.ofValue(0);

        assertEquals(0, wm.getValue());
        assertEquals(0, wm2.getValue());
        assertEquals(Watermark.LOWEST, wm2);

        try {
            Watermark.ofTimestamp(-10);
            fail("exception expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            Watermark.ofValue(-10);
            fail("exception expected");
        } catch (IllegalArgumentException e) {
            // expected
        }

        // check that the timestamp is not truncated even in 2042
        long t0 = 2299795200000L;
        wm = Watermark.ofTimestamp(t0);
        wm2 = Watermark.ofValue(wm.getValue());
        assertEquals(wm, wm2);
        assertEquals(t0, wm2.getTimestamp());

        // completed of 0 is possible
        wm = Watermark.ofTimestamp(0, (short) 0);
        wm2 = Watermark.completedOf(wm);
        assertTrue(wm.compareTo(wm2) < 0);
    }

    @Test
    public void testWmComparable() {
        Watermark wm0 = Watermark.ofTimestamp(2);
        Watermark wm1 = Watermark.ofTimestamp(2, (short) 1);
        Watermark wm2 = Watermark.ofTimestamp(3);
        Watermark wm2bis = Watermark.ofTimestamp(3, (short) 0);
        Watermark wm3 = Watermark.ofTimestamp(3, (short) 0);
        wm3 = Watermark.completedOf(wm3);

        assertEquals(wm0, wm0);
        assertEquals(wm2, wm2bis);
        assertEquals(0, wm2bis.compareTo(wm2));

        assertTrue(wm0.compareTo(wm1) < 0);
        assertTrue(wm0.compareTo(wm2) < 0);
        assertTrue(wm0.compareTo(wm3) < 0);

        assertTrue(wm1.compareTo(wm2) < 0);
        assertTrue(wm1.compareTo(wm3) < 0);
        assertTrue(wm2.compareTo(wm3) < 0);
        assertTrue(wm2bis.compareTo(wm3) < 0);

        assertTrue(wm3.compareTo(wm2bis) > 0);
        assertTrue(wm2bis.compareTo(wm1) > 0);
        assertTrue(wm3.compareTo(wm0) > 0);

        // test case of long overflow
        wm0 = Watermark.ofValue(195409460902363138L);
        wm1 = Watermark.ofTimestamp(1);
        assertTrue(wm0.compareTo(wm1) > 0);

    }

    // WatermarkMonotonicInterval tests

    @Test
    public void testWmiIsDone() {
        WatermarkMonotonicInterval wmi = new WatermarkMonotonicInterval();
        wmi.mark(Watermark.ofTimestamp(10));
        assertTrue(wmi.isDone(9));
        assertFalse(wmi.isDone(10));
        assertFalse(wmi.isDone(11));
        Assert.assertEquals(10, wmi.getLow().getTimestamp());
        Assert.assertEquals(10, wmi.getHigh().getTimestamp());
    }

    @Test
    public void testWmiAdjustOnNullWatermark() {
        WatermarkMonotonicInterval wmi = new WatermarkMonotonicInterval();
        long low = wmi.mark(200000);
        assertEquals(200000, low);
        assertTrue(wmi.getLow().getTimestamp() > 0);
    }

    @Test
    public void testWmiMarkAndCheckpoint() {
        long t0 = System.currentTimeMillis();
        long t1 = t0 + 1;
        long t2 = t0 + 2;
        long t3 = t0 + 3;

        // watermark associated with timestamp
        long w0 = Watermark.ofTimestamp(t0).getValue();
        long w1 = Watermark.ofTimestamp(t1).getValue();
        long w2 = Watermark.ofTimestamp(t2).getValue();
        long w3 = Watermark.ofTimestamp(t3).getValue();

        WatermarkMonotonicInterval wmi = new WatermarkMonotonicInterval();
        wmi.mark(w2);
        wmi.mark(w3);
        wmi.mark(w2);
        wmi.mark(w0);
        wmi.mark(w1);
        // the low watermark is w0
        Assert.assertEquals(w0, wmi.getLow().getValue());
        Assert.assertEquals(w3, wmi.getHigh().getValue());

        // no checkpoint occurs so nothing is done
        assertFalse(wmi.getLow().isCompleted());
        assertFalse(wmi.isDone(t0));
        assertFalse(wmi.isDone(t3));

        wmi.checkpoint();
        // low watermark is now to w3 completed
        assertTrue(wmi.getLow().isCompleted());
        assertTrue(wmi.isDone(t0));
        assertTrue(wmi.isDone(t2));
        assertTrue(wmi.isDone(t3));
        assertTrue(wmi.getLow().getValue() > w3);
        Assert.assertEquals(t3, wmi.getLow().getTimestamp());

        // we can not go lower than the last checkpoint value
        wmi.mark(w0);
        Assert.assertEquals(t3, wmi.getLow().getTimestamp());
        assertTrue(wmi.isDone(t3));

        wmi.checkpoint();
        assertTrue(wmi.getLow().isCompleted());
        Assert.assertEquals(t3, wmi.getLow().getTimestamp());
        assertTrue(wmi.isDone(t3));
    }

    @Test
    public void testWmiMarkAndCheckpointWithSequence() {
        // long t0 = System.currentTimeMillis();
        long t0 = 1490855872363L; // fail
        // long t0 = 1490855953208L; // ok
        long t1 = t0 + 1;
        long w0 = Watermark.ofTimestamp(t0).getValue();
        long w1 = Watermark.ofTimestamp(t0, (short) 1).getValue();
        long w2 = Watermark.ofTimestamp(t0, (short) 2).getValue();
        long w3 = Watermark.ofTimestamp(t0, (short) 3).getValue();

        WatermarkMonotonicInterval wmi = new WatermarkMonotonicInterval();
        wmi.mark(w2);
        wmi.mark(w0);
        wmi.mark(w1);
        wmi.mark(w3);

        Assert.assertEquals(w0, wmi.getLow().getValue());
        assertFalse(wmi.getLow().isCompleted());
        assertFalse(wmi.isDone(t0));

        wmi.checkpoint();
        Assert.assertEquals(t0, wmi.getLow().getTimestamp());
        assertTrue(wmi.isDone(t0));
        assertFalse(wmi.isDone(t1));
        Assert.assertEquals(w3 + 1, wmi.getLow().getValue());

        // mark with a smaller wm, but low stay on last checkpoint
        wmi.mark(w1);
        Assert.assertEquals(w3 + 1, wmi.getLow().getValue());

        wmi.checkpoint();
        Assert.assertEquals(w3 + 1, wmi.getLow().getValue());
    }

    @Test
    public void testWmiLowWatermarkWithSequenceCheckpoint() {
        long t0 = System.currentTimeMillis();
        long w0 = Watermark.ofTimestamp(t0).getValue();
        long w1 = Watermark.ofTimestamp(t0, (short) 1).getValue();
        long w2 = Watermark.ofTimestamp(t0, (short) 2).getValue();

        WatermarkMonotonicInterval wmi = new WatermarkMonotonicInterval();
        wmi.mark(w0);
        wmi.checkpoint();

        wmi.mark(w1);
        wmi.checkpoint();
        assertTrue(wmi.getLow().getValue() > w1);
        assertTrue(w2 > wmi.getLow().getValue());
        // bis
        wmi.mark(w1);
        wmi.checkpoint();
        assertTrue(wmi.getLow().getValue() > w1);
        assertTrue(w2 > wmi.getLow().getValue());

    }

    @Test
    public void testWmiLimits() {
        WatermarkMonotonicInterval wmi = new WatermarkMonotonicInterval();
        // checkpoint without mark
        wmi.checkpoint();
        assertTrue(wmi.getLow().isCompleted());

        // default low and high
        wmi = new WatermarkMonotonicInterval();
        Assert.assertEquals(Watermark.LOWEST, wmi.getLow());
        Assert.assertEquals(Watermark.LOWEST, wmi.getHigh());

        // mark lowest
        wmi = new WatermarkMonotonicInterval();
        wmi.mark(Watermark.LOWEST);
        long low = wmi.checkpoint();
        assertTrue(wmi.getLow().isCompleted());

        // double checkpoint
        wmi.checkpoint();
        wmi.checkpoint();
        assertTrue(wmi.getLow().isCompleted());
        Assert.assertEquals(low, wmi.getLow().getValue());

        // initial value and checkpoint
        wmi = new WatermarkMonotonicInterval();
        Assert.assertEquals(Watermark.LOWEST, wmi.getLow());
        Assert.assertEquals(Watermark.LOWEST, wmi.getHigh());
        wmi.checkpoint();
        Assert.assertEquals(1, wmi.getLow().getValue());
        Assert.assertEquals(Watermark.LOWEST, wmi.getHigh());
        wmi.mark(0);
        Assert.assertEquals(1, wmi.getLow().getValue());
        Assert.assertEquals(Watermark.LOWEST, wmi.getHigh());

        long t0 = System.currentTimeMillis();
        wmi.mark(Watermark.ofTimestamp(t0));
        Assert.assertEquals(t0, wmi.getHigh().getTimestamp());
        Assert.assertEquals(1, wmi.getLow().getValue());

    }

    // test monotonic wmi
    @Test
    public void testWmmi() {
        WatermarkMonotonicInterval wmi = new WatermarkMonotonicInterval();
        long t0 = System.currentTimeMillis();
        Watermark w0 = Watermark.ofTimestamp(t0);
        Watermark w1 = Watermark.ofTimestamp(t0, (short) 1);
        Watermark w2 = Watermark.ofTimestamp(t0, (short) 2);
        Watermark w3 = Watermark.ofTimestamp(t0, (short) 3);

        wmi.mark(w1);
        wmi.mark(w0);
        wmi.mark(w2);
        Assert.assertEquals(w0, wmi.getLow());

        wmi.checkpoint();
        Assert.assertEquals(Watermark.completedOf(w2), wmi.getLow());
        wmi.mark(w3);
        wmi.mark(w2);
        wmi.mark(w1);
        // discard w1 and w2 because they are lower than checkpoint wm
        Assert.assertEquals(Watermark.completedOf(w2), wmi.getLow());

        wmi.checkpoint();
        Assert.assertEquals(Watermark.completedOf(w3), wmi.getLow());
    }

}
