/*
 * (C) Copyright 2013-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 *     tmartins
 */
package org.nuxeo.ecm.core.schema.utils;

import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;

/**
 * @author dmetzler
 */
public class DateParserTest {

    @Test
    public void parseTestDate() throws Exception {
        // SUPNXP-7186 that threw an exception
        Calendar cal = DateParser.parse("2012-11-29T13:15:18.176+0000");
        assertEquals(13, cal.get(Calendar.HOUR_OF_DAY));

        cal = DateParser.parse("2012-11-29T13:15:18.176+0200");
        assertEquals(11, cal.get(Calendar.HOUR_OF_DAY));

        cal = DateParser.parse("0002-11-29T23:00:00.00Z");
        assertEquals(2, cal.get(Calendar.YEAR));
        assertEquals(Calendar.NOVEMBER, cal.get(Calendar.MONTH));
    }

    @Test
    public void testFormatDate() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(0);
        assertEquals("1970-01-01T00:00:00.000Z", DateParser.formatW3CDateTime(cal));
        cal.setTimeInMillis(1486660428314L);
        assertEquals("2017-02-09T17:13:48.314Z", DateParser.formatW3CDateTime(cal));
    }

    @Test
    public void testReverseParsingDate() throws Exception {
        // NXP-19153: force time zone
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        // first test with date set to zero
        Date d = new Date(0);
        String s = DateParser.formatW3CDateTime(d);

        Calendar cal = Calendar.getInstance();
        // test String to Date
        cal.setTime(DateParser.parseW3CDateTime(s));
        assertEquals(1970, cal.get(Calendar.YEAR));
        assertEquals(0, cal.get(Calendar.MONTH));

        // test String to Calendar
        cal = DateParser.parse(s);
        assertEquals(1970, cal.get(Calendar.YEAR));
        assertEquals(0, cal.get(Calendar.MONTH));
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));

        // then test with a regular calendar 2/1/1
        Calendar gcal = Calendar.getInstance();
        gcal.set(Calendar.YEAR, 2);
        gcal.set(Calendar.MONTH, 1); // second month = February
        gcal.set(Calendar.DAY_OF_MONTH, 1);
        s = DateParser.formatW3CDateTime(gcal.getTime());

        cal.setTime(DateParser.parseW3CDateTime(s));
        assertEquals(gcal.get(Calendar.YEAR), cal.get(Calendar.YEAR));
        assertEquals(Calendar.FEBRUARY, cal.get(Calendar.MONTH));

        cal = DateParser.parse(s);
        assertEquals(2, cal.get(Calendar.YEAR));
        assertEquals(1, cal.get(Calendar.MONTH));
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void parseTimeZone() throws Exception {
        Calendar cal = DateParser.parse("2012-11-29T13:15:18.176Z");
        assertEquals(13, cal.get(Calendar.HOUR_OF_DAY));

        cal = DateParser.parse("2012-11-29T13:15:18.176+0100");
        assertEquals(12, cal.get(Calendar.HOUR_OF_DAY));

        cal = DateParser.parse("2012-11-29T13:15:18.176+01:00");
        assertEquals(12, cal.get(Calendar.HOUR_OF_DAY));

        cal = DateParser.parse("2012-11-29T13:15:18.176+01");
        assertEquals(12, cal.get(Calendar.HOUR_OF_DAY));

        cal = DateParser.parse("2012-11-29T13:15:18.176-0100");
        assertEquals(14, cal.get(Calendar.HOUR_OF_DAY));
    }
}
