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
 *     Florent Guillaume
 */
package org.nuxeo.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.time.format.DateTimeParseException;

import org.junit.Test;

public class TestDurationUtils {

    protected static final Duration parse(String s) {
        return DurationUtils.parse(s);
    }

    protected static final Duration parsePositive(String s) {
        return DurationUtils.parsePositive(s, Duration.ofDays(-123));
    }

    @Test
    public void testParse() {
        // ISO 8601 format
        assertEquals(Duration.ofSeconds(20).plusMillis(345), parse("PT20.345S"));
        assertEquals(Duration.ofMinutes(15), parse("PT15M"));
        assertEquals(Duration.ofHours(10), parse("PT10H"));
        assertEquals(Duration.ofDays(2), parse("P2D"));
        assertEquals(Duration.ofDays(2).plusHours(3).plusMinutes(4), parse("P2DT3H4M"));
        assertEquals(Duration.ofHours(-6).plusMinutes(3), parse("PT-6H3M"));
        assertEquals(Duration.ofHours(-6).plusMinutes(-3), parse("-PT6H3M"));
        assertEquals(Duration.ofHours(6).plusMinutes(-3), parse("-PT-6H+3M"));
        // custom format
        assertEquals(Duration.ofMillis(123), parse("123ms"));
        assertEquals(Duration.ofSeconds(88), parse("88s"));
        assertEquals(Duration.ofMinutes(15), parse("15m"));
        assertEquals(Duration.ofHours(10), parse("10h"));
        assertEquals(Duration.ofDays(2), parse("2d"));
        assertEquals(Duration.ofDays(2).plusHours(3).plusMinutes(4).plusSeconds(5).plusMillis(6), parse("2d3h4m5s6ms"));
        // bad format
        try {
            parse("1 day");
            fail();
        } catch (DateTimeParseException e) {
            assertEquals("Text cannot be parsed to a Duration", e.getMessage());
            assertEquals("1 day", e.getParsedString());
        }
    }

    @Test
    public void testParsePositive() {
        assertEquals(Duration.ofSeconds(20), parsePositive("PT20S"));
        assertEquals(Duration.ofSeconds(20), parsePositive("20s"));
        // use default
        assertEquals(Duration.ofDays(-123), parsePositive(" ")); // blank
        assertEquals(Duration.ofDays(-123), parsePositive("0s")); // zero
        assertEquals(Duration.ofDays(-123), parsePositive("-PT20S")); // negative
        assertEquals(Duration.ofDays(-123), parsePositive("1 day")); // bad format
    }

}
