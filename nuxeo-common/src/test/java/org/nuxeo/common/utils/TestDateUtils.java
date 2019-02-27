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
 *     pierre
 */
package org.nuxeo.common.utils;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.common.utils.DateUtils.parseISODateTime;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

/**
 * @since 11.1
 */
public class TestDateUtils {

    private static final Map<String, String> ACTUAL_TO_EXPECTED = new HashMap<>();

    static {
        ACTUAL_TO_EXPECTED.put("1985-04-12T23:20:50.52Z", "1985-04-12T23:20:50.520Z");
        ACTUAL_TO_EXPECTED.put("1986-04-12T19:20:50.52-04:00", "1986-04-12T23:20:50.520Z");
        ACTUAL_TO_EXPECTED.put("1987-10-11T22:14:15.003Z", "1987-10-11T22:14:15.003Z");
        ACTUAL_TO_EXPECTED.put("1988-08-24T05:14:15.000003-07:00", "1988-08-24T12:14:15.000Z");
        ACTUAL_TO_EXPECTED.put("1989-04-13T11:11:11-08:00", "1989-04-13T19:11:11.000Z");
        ACTUAL_TO_EXPECTED.put("1990-04-13T08:08:08.0001+00:00", "1990-04-13T08:08:08.000Z");
        ACTUAL_TO_EXPECTED.put("1991-04-13T08:08:08.251+00:00", "1991-04-13T08:08:08.251Z");
        ACTUAL_TO_EXPECTED.put("1992-04-13T08:08:08.008+09:00", "1992-04-12T23:08:08.008Z");
        ACTUAL_TO_EXPECTED.put("1993", "1993-01-01T00:00:00.000Z");
        ACTUAL_TO_EXPECTED.put("1994-03", "1994-03-01T00:00:00.000Z");
        ACTUAL_TO_EXPECTED.put("1995-03-12", "1995-03-12T00:00:00.000Z");
        ACTUAL_TO_EXPECTED.put("1996-03-12T12", "1996-03-12T12:00:00.000Z");
        ACTUAL_TO_EXPECTED.put("1997-02-03T12:34", "1997-02-03T12:34:00.000Z");
        ACTUAL_TO_EXPECTED.put("1998-08-08T12:34:56", "1998-08-08T12:34:56.000Z");
        ACTUAL_TO_EXPECTED.put("1999-08-08T12:34:56.789", "1999-08-08T12:34:56.789Z");
        ACTUAL_TO_EXPECTED.put("2000-02-22T14:32:55.188+01:00[Europe/Paris]", "2000-02-22T13:32:55.188Z");
    }

    @Test
    public void testParse() {
        for (Entry<String, String> input : ACTUAL_TO_EXPECTED.entrySet()) {
            ZonedDateTime zdt = parseISODateTime(input.getKey());
            ZonedDateTime expected = parseISODateTime(input.getValue());
            assertEquals(DateUtils.formatISODateTime(zdt), input.getValue());
            assertEquals(zdt.toInstant().toEpochMilli(), expected.toInstant().toEpochMilli());
        }
    }

}
