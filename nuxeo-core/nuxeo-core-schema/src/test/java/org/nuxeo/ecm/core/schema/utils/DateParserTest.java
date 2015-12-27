/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.schema.utils;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;

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
