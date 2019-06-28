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

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.time.temporal.ChronoUnit.NANOS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.time.temporal.ChronoUnit.YEARS;
import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

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

    @Test
    public void testTemporalAmountGetUnits() {
        assertEquals(List.of(YEARS, MONTHS, DAYS, SECONDS, NANOS), PeriodAndDuration.ZERO.getUnits());
    }

    @Test
    public void testTemporalAmountGet() {
        PeriodAndDuration pd = PeriodAndDuration.parse("P1Y2M3DT4H5M6.007S");
        assertEquals(1, pd.get(YEARS));
        assertEquals(2, pd.get(MONTHS));
        assertEquals(3, pd.get(DAYS));
        assertEquals(4 * 60 * 60 + 5 * 60 + 6, pd.get(SECONDS));
        assertEquals(7_000_000, pd.get(NANOS));
    }

    @Test
    public void testTemporalAmountAddToSubstractFrom() {
        PeriodAndDuration pd = PeriodAndDuration.parse("P1Y2M3DT4H5M6.007S");
        ZonedDateTime dt = ZonedDateTime.parse("2001-02-03T04:05:06.007Z");
        assertEquals("2002-04-06T08:10:12.014Z", pd.addTo(dt).toString());
        assertEquals("1999-11-30T00:00Z", pd.subtractFrom(dt).toString());
        // TemporalAmount.addTo/subtractFrom are indirectly called when using Temporal.plus/minus
        assertEquals("2002-04-06T08:10:12.014Z", dt.plus(pd).toString());
        assertEquals("1999-11-30T00:00Z", dt.minus(pd).toString());
    }

}
