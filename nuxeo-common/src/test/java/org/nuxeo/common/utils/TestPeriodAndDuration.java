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
 *     Florent Guillaume
 */
package org.nuxeo.common.utils;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.time.Period;
import java.util.Arrays;

import org.junit.Test;

public class TestPeriodAndDuration {

    @Test
    public void testConstructorsAndToString() {
        Period period = Period.ZERO.plusYears(1).plusMonths(2).plusDays(3);
        Duration duration = Duration.ZERO.plusHours(4).plusMinutes(5).plusSeconds(6);
        PeriodAndDuration pd = new PeriodAndDuration(period);
        assertEquals("P1Y2M3D", pd.toString());
        pd = new PeriodAndDuration(duration);
        assertEquals("PT4H5M6S", pd.toString());
        pd = new PeriodAndDuration(period, duration);
        assertEquals("P1Y2M3DT4H5M6S", pd.toString());
    }

    @Test
    public void testParseThenToString() {
        checkParseThenToString("PT0S");
        for (String text : Arrays.asList("P1Y2M3D", "PT4H5M6S", "P1Y2M3DT4H5M6S", "PT1.234567S")) {
            checkParseThenToString(text);
            checkParseThenToString("-" + text);
        }
    }

    protected static void checkParseThenToString(String text) {
        PeriodAndDuration pd = PeriodAndDuration.parse(text);
        assertEquals(text, pd.toString());
    }

}
