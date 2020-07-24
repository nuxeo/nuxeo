/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.storage.marklogic;

import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.JULY;
import static java.util.Calendar.MARCH;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.SECOND;
import static java.util.Calendar.YEAR;
import static org.joda.time.DateTimeZone.UTC;
import static org.junit.Assert.assertEquals;

import java.util.Calendar;

import org.joda.time.DateTimeZone;
import org.junit.Test;

/**
 * @since 10.10-HF
 */
public class TestMarkLogicHelper {

    private static final DateTimeZone EUROPE_PARIS = DateTimeZone.forID("Europe/Paris");

    @Test
    public void testSerializeCalendar() {
        testSerializeCalendar("TimeZone: NA", null);
        testSerializeCalendar("TimeZone: UTC", UTC);
        testSerializeCalendar("TimeZone: Europe/Paris", EUROPE_PARIS);
    }

    protected void testSerializeCalendar(String message, DateTimeZone dateTimeZone) {
        // use the default time zone from joda as we're using joda to serialize/deserialize
        // difference between JDK default is that joda returns a SimpleTimeZone instead of a TimeZone object
        Calendar calendar = dateTimeZone == null ? Calendar.getInstance(DateTimeZone.getDefault().toTimeZone())
                : Calendar.getInstance(dateTimeZone.toTimeZone());
        calendar.set(2020, JULY, 24, 12, 59, 34);
        calendar.set(MILLISECOND, 459);

        String calendarString = dateTimeZone == null ? MarkLogicHelper.serializeCalendar(calendar)
                : MarkLogicHelper.serializeCalendar(calendar, dateTimeZone);
        assertEquals(message, "2020-07-24T12:59:34.459", calendarString);
    }

    @Test
    public void testDeserializeCalendar() {
        testDeserializeCalendar("TimeZone: NA", null);
        testDeserializeCalendar("TimeZone: UTC", UTC);
        testDeserializeCalendar("TimeZone: Europe/Paris", EUROPE_PARIS);
    }

    protected void testDeserializeCalendar(String message, DateTimeZone dateTimeZone) {
        String calendarString = "2020-07-24T12:59:34.459";
        Calendar calendar = dateTimeZone == null ? MarkLogicHelper.deserializeCalendar(calendarString)
                : MarkLogicHelper.deserializeCalendar(calendarString, dateTimeZone);
        assertEquals(message, 2020, calendar.get(YEAR));
        assertEquals(message, JULY, calendar.get(MONTH));
        assertEquals(message, 24, calendar.get(DAY_OF_MONTH));
        assertEquals(message, 12, calendar.get(HOUR_OF_DAY));
        assertEquals(message, 59, calendar.get(MINUTE));
        assertEquals(message, 34, calendar.get(SECOND));
        assertEquals(message, 459, calendar.get(MILLISECOND));
    }

    @Test
    public void testSerializeDeserializeCalendar() {
        testSerializeDeserializeCalendar("TimeZone: NA", null);
        testSerializeDeserializeCalendar("TimeZone: UTC", UTC);
        testSerializeDeserializeCalendar("TimeZone: Europe/Paris", EUROPE_PARIS);
    }

    protected void testSerializeDeserializeCalendar(String message, DateTimeZone dateTimeZone) {
        // use the default time zone from joda as we're using joda to serialize/deserialize
        // difference between JDK default is that joda returns a SimpleTimeZone instead of a TimeZone object
        Calendar calendar = dateTimeZone == null ? Calendar.getInstance(DateTimeZone.getDefault().toTimeZone())
                : Calendar.getInstance(dateTimeZone.toTimeZone());
        calendar.set(2020, JULY, 24, 12, 59, 34);
        calendar.set(MILLISECOND, 459);

        String calendarString = dateTimeZone == null ? MarkLogicHelper.serializeCalendar(calendar)
                : MarkLogicHelper.serializeCalendar(calendar, dateTimeZone);
        Calendar deserializedCalendar = dateTimeZone == null ? MarkLogicHelper.deserializeCalendar(calendarString)
                : MarkLogicHelper.deserializeCalendar(calendarString, dateTimeZone);

        assertEquals(message, calendar, deserializedCalendar);
    }

    @Test
    public void testDeserializeInvalidCalendarRegardingDST() {
        Calendar calendar = MarkLogicHelper.deserializeCalendar("2020-03-29T02:39:46.058", EUROPE_PARIS);
        assertEquals(2020, calendar.get(YEAR));
        assertEquals(MARCH, calendar.get(MONTH));
        assertEquals(29, calendar.get(DAY_OF_MONTH));
        assertEquals(3, calendar.get(HOUR_OF_DAY)); // 3h instead of 2h due to DST
        assertEquals(39, calendar.get(MINUTE));
        assertEquals(46, calendar.get(SECOND));
        assertEquals(58, calendar.get(MILLISECOND));
    }

}
