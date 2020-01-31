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

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Java 8 time utilities.
 *
 * @since 11.1
 */
public class DateUtils {

    public static final String ISODATETIME_GENERIC_PATTERN = "yyyy[-MM][-dd['T'HH[:mm[:ss[.SSS]]]]][XXX]";

    public static final DateTimeFormatter ISO_ROBUST_DATE_TIME = robustOfPattern(ISODATETIME_GENERIC_PATTERN);

    public static final DateTimeFormatter[] formatters = {
            ISO_OFFSET_DATE_TIME,
            ISO_ZONED_DATE_TIME,
            ISO_ROBUST_DATE_TIME, };

    public static final DateTimeFormatter ISO_DATE_ONLY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private DateUtils() {
        // utility class
    }

    public static String formatISODateTime(Calendar calendar) {
        return formatISODateTime(toZonedDateTime(calendar));
    }

    public static String formatISODateTime(Date date) {
        return formatISODateTime(toZonedDateTime(date));
    }

    public static String formatISODateTime(ZonedDateTime zdt) {
        return formatISODateTime(zdt, false);
    }

    public static String formatISODateTime(ZonedDateTime zdt, boolean dateOnly) {
        if (zdt == null) {
            return null;
        }
        if (dateOnly) {
            return ISO_DATE_ONLY.format(zdt);
        }
        return ISO_ROBUST_DATE_TIME.format(zdt);
    }

    public static Date nowIfNull(Date date) {
        return date == null ? new Date() : date;
    }

    public static Calendar nowIfNull(Calendar calendar) {
        return calendar == null ? Calendar.getInstance() : calendar;
    }

    public static ZonedDateTime nowIfNull(ZonedDateTime zdt) {
        return zdt == null ? ZonedDateTime.now() : zdt;
    }

    public static ZonedDateTime parseISODateTime(String string) {
        return parse(string, formatters);
    }

    public static ZonedDateTime parse(String string, DateTimeFormatter... formatters) {
        // workaround to allow space instead of T after the date part
        if (string.length() > 10 && string.charAt(10) == ' ') {
            char[] s = string.toCharArray();
            s[10] = 'T';
            string = new String(s);
        }
        for (DateTimeFormatter formatter : formatters) {
            try {
                TemporalAccessor ta = formatter.parseBest(string, ZonedDateTime::from, LocalDate::from);
                if (ta instanceof ZonedDateTime) {
                    return (ZonedDateTime) ta;
                } else {
                    return ((LocalDate) ta).atStartOfDay(ZoneOffset.UTC);
                }
            } catch (Exception e) {
                // ignore, try another formatter
            }
        }
        throw new DateTimeException("Could not parse '" + string + "'");
    }

    public static final DateTimeFormatter robustOfPattern(String pattern) {
        return new DateTimeFormatterBuilder().appendPattern(pattern)
                                             .parseDefaulting(MONTH_OF_YEAR, 1)
                                             .parseDefaulting(DAY_OF_MONTH, 1)
                                             .parseDefaulting(HOUR_OF_DAY, 0)
                                             .parseDefaulting(MINUTE_OF_HOUR, 0)
                                             .parseDefaulting(SECOND_OF_MINUTE, 0)
                                             .parseDefaulting(NANO_OF_SECOND, 0)
                                             .toFormatter()
                                             .withZone(ZoneOffset.UTC);
    }

    public static Calendar toCalendar(Instant instant) {
        if (instant == null) {
            return null;
        }
        // an Instant is on UTC by definition
        var zdt = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
        return GregorianCalendar.from(zdt);
    }

    public static Date toDate(ZonedDateTime zdt) {
        if (zdt == null) {
            return null;
        }
        return Date.from(zdt.toInstant());
    }

    public static Instant toInstant(Calendar calendar) {
        if (calendar == null) {
            return null;
        }
        return calendar.toInstant();
    }

    public static ZonedDateTime toZonedDateTime(Calendar calendar) {
        if (calendar == null) {
            return null;
        }
        return ZonedDateTime.ofInstant(calendar.toInstant(), ZoneOffset.UTC);
    }

    public static ZonedDateTime toZonedDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return ZonedDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
    }

}
