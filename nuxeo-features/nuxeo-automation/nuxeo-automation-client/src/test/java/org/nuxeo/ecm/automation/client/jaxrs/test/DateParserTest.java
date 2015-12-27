/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.jaxrs.test;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.automation.client.model.DateParser;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DateParserTest {

    @Test
    public void testEncodingDecodingMs() {
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 1000; i++) {
            cal.set(Calendar.MILLISECOND, i);
            Date date = cal.getTime();
            String encoded = DateParser.formatW3CDateTimeMs(date);
            Date decoded = DateParser.parseW3CDateTime(encoded);
            assertEquals(date, decoded);
        }
    }

    @Test
    public void testEncodingDecoding() {
        Date date = new Date();
        String encoded = DateParser.formatW3CDateTime(date);
        Date decoded = DateParser.parseW3CDateTime(encoded);
        // the dates cannot be the same since milliseconds information was
        // removed
        // they are the same up to the seconds field.
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.MILLISECOND, 0);
        Date dateNoMs = cal.getTime();
        assertEquals(dateNoMs, decoded);

        // when formatting including milliseconds the decoded date is equals to
        // the initial one
        encoded = DateParser.formatW3CDateTimeMs(date);
        decoded = DateParser.parseW3CDateTime(encoded);
        assertEquals(date, decoded);
    }

    /**
     * Test the parser using all the specification accepted formats
     */
    @Test
    public void testParser1() throws Exception {
        // YYYY-MM-DDThh:mm:ss.sTZD => 1997-07-16T19:20:30.45+02:00
        Calendar ref = Calendar.getInstance(TimeZone.getTimeZone("GMT+2:00"));
        ref.set(1997, 06, 16, 19, 20, 30);
        ref.set(Calendar.MILLISECOND, 45);
        Calendar cal = DateParser.parse("1997-07-16T19:20:30.045+02:00");
        assertEquals(ref.getTime(), cal.getTime());
        // calendars are not equals since they have different time zones - but
        // datetime in milliseconds are the same
    }

    @Test
    public void testParser2() throws Exception {
        // YYYY-MM-DDThh:mm:ss.sTZD => 1997-07-16T19:20:30.45-02:00
        Calendar ref = Calendar.getInstance(TimeZone.getTimeZone("GMT-2:00"));
        ref.set(1997, 06, 16, 19, 20, 30);
        ref.set(Calendar.MILLISECOND, 45);
        Calendar cal = DateParser.parse("1997-07-16T19:20:30.045-02:00");
        assertEquals(ref.getTime(), cal.getTime());
        // calendars are not equals since they have different time zones - but
        // datetime in milliseconds are the same
    }

    @Test
    public void testParser3() throws Exception {
        // YYYY-MM-DDThh:mm:ss.sTZD => 1997-07-16T19:20:30.45Z
        Calendar ref = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        ref.set(1997, 06, 16, 19, 20, 30);
        ref.set(Calendar.MILLISECOND, 45);
        Calendar cal = DateParser.parse("1997-07-16T19:20:30.045Z");
        assertEquals(ref.getTime(), cal.getTime());
        // calendars are not equals since they have different time zones - but
        // datetime in milliseconds are the same
    }

    @Test
    public void testParser4() throws Exception {
        // YYYY-MM-DDThh:mm:ssTZD => 1997-07-16T19:20:30Z
        Calendar ref = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        ref.set(1997, 06, 16, 19, 20, 30);
        ref.set(Calendar.MILLISECOND, 0);
        Calendar cal = DateParser.parse("1997-07-16T19:20:30Z");
        assertEquals(ref.getTime(), cal.getTime());
        // calendars are not equals since they have different time zones - but
        // datetime in milliseconds are the same
    }

    @Test
    public void testParser5() throws Exception {
        // YYYY-MM-DDThh:mmTZD => 1997-07-16T19:20Z
        Calendar ref = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        ref.set(1997, 06, 16, 19, 20, 0);
        ref.set(Calendar.MILLISECOND, 0);
        Calendar cal = DateParser.parse("1997-07-16T19:20Z");
        assertEquals(ref.getTime(), cal.getTime());
        // calendars are not equals since they have different time zones - but
        // datetime in milliseconds are the same
    }

    @Test
    public void testParser6() throws Exception {
        // YYYY-MM-DD => 1997-07-16
        Calendar ref = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        ref.set(1997, 06, 16);
        Calendar cal = DateParser.parse("1997-07-16");
        assertEquals(ref.get(Calendar.YEAR), cal.get(Calendar.YEAR));
        assertEquals(ref.get(Calendar.MONTH), cal.get(Calendar.MONTH));
        assertEquals(ref.get(Calendar.DATE), cal.get(Calendar.DATE));
    }

    @Test
    public void testParser7() throws Exception {
        // YYYY-MM => 1997-07
        Calendar ref = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        ref.set(1997, 06, 1);
        Calendar cal = DateParser.parse("1997-07");
        assertEquals(ref.get(Calendar.YEAR), cal.get(Calendar.YEAR));
        assertEquals(ref.get(Calendar.MONTH), cal.get(Calendar.MONTH));
    }

    @Test
    public void testParser8() throws Exception {
        // YYYY => 1997
        Calendar ref = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        ref.set(Calendar.YEAR, 1997);
        Calendar cal = DateParser.parse("1997");
        assertEquals(ref.get(Calendar.YEAR), cal.get(Calendar.YEAR));
    }

}
