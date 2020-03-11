/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.core.schema.utils;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Parse / format ISO 8601 dates.
 *
 * @author "Stephane Lacoin [aka matic] <slacoin at nuxeo.com>"
 */
public class DateParser {

    /**
     * @since 8.2
     */
    public static final String W3C_DATE_FORMAT = "%04d-%02d-%02dT%02d:%02d:%02d.%03dZ";

    public static Calendar parse(String str) throws ParseException {
        if (str == null) {
            return null;
        }
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.clear();
        int len = str.length();
        if (len == 0) { // empty string
            // TODO throw error?
            return cal;
        }
        int i = 0;
        i = readYear(cal, str, i);
        i = readCharOpt('-', cal, str, i);
        if (i == -1) {
            return cal;
        }
        i = readMonth(cal, str, i);
        i = readCharOpt('-', cal, str, i);
        if (i == -1) {
            return cal;
        }
        i = readDay(cal, str, i);
        i = readCharOpt('T', cal, str, i);
        if (i == -1) {
            return cal;
        }
        i = readHours(cal, str, i);
        i = readCharOpt(':', cal, str, i);
        if (i == -1) {
            return cal;
        }
        i = readMinutes(cal, str, i);
        if (isChar(':', str, i)) {
            i = readSeconds(cal, str, i + 1);
            if (isChar('.', str, i)) {
                i = readMilliseconds(cal, str, i + 1);
            }
        }
        if (i > -1) {
            readTimeZone(cal, str, i);
        }
        return cal;
    }

    public static Date parseW3CDateTime(String str) {
        if (str == null || "".equals(str)) {
            return null;
        }
        try {
            return parse(str).getTime();
        } catch (ParseException e) {
            throw new IllegalArgumentException("Failed to parse ISO 8601 date: " + str, e);
        }
    }

    /**
     * 2011-10-23T12:00:00.000Z
     *
     * @param date
     * @return
     */
    public static String formatW3CDateTime(Date date) {
        if (date == null) {
            return null;
        }
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTime(date);
        return formatW3CDateTime(cal);
    }

    /**
     * 2011-10-23T12:00:00.000Z
     *
     * @param calendar
     * @return
     *
     * @since 7.3
     */
    public static String formatW3CDateTime(Calendar calendar) {
        if (calendar == null) {
            return null;
        }
        return String.format(W3C_DATE_FORMAT, calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DATE),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND),
                calendar.get(Calendar.MILLISECOND));
    }

    private final static int readYear(Calendar cal, String str, int off) throws ParseException {
        if (str.length() >= off + 4) {
            cal.set(Calendar.YEAR, Integer.parseInt(str.substring(off, off + 4)));
            return off + 4;
        }
        throw new ParseException("Invalid year in date '" + str + "'", off);
    }

    private final static int readMonth(Calendar cal, String str, int off) throws ParseException {
        if (str.length() >= off + 2) {
            cal.set(Calendar.MONTH, Integer.parseInt(str.substring(off, off + 2)) - 1);
            return off + 2;
        }
        throw new ParseException("Invalid month in date '" + str + "'", off);
    }

    private final static int readDay(Calendar cal, String str, int off) throws ParseException {
        if (str.length() >= off + 2) {
            cal.set(Calendar.DATE, Integer.parseInt(str.substring(off, off + 2)));
            return off + 2;
        }
        throw new ParseException("Invalid day in date '" + str + "'", off);
    }

    private final static int readHours(Calendar cal, String str, int off) throws ParseException {
        if (str.length() >= off + 2) {
            cal.set(Calendar.HOUR, Integer.parseInt(str.substring(off, off + 2)));
            return off + 2;
        }
        throw new ParseException("Invalid hours in date '" + str + "'", off);
    }

    private final static int readMinutes(Calendar cal, String str, int off) throws ParseException {
        if (str.length() >= off + 2) {
            cal.set(Calendar.MINUTE, Integer.parseInt(str.substring(off, off + 2)));
            return off + 2;
        }
        throw new ParseException("Invalid minutes in date '" + str + "'", off);
    }

    private final static int readSeconds(Calendar cal, String str, int off) throws ParseException {
        if (str.length() >= off + 2) {
            cal.set(Calendar.SECOND, Integer.parseInt(str.substring(off, off + 2)));
            return off + 2;
        }
        throw new ParseException("Invalid seconds in date '" + str + "'", off);
    }

    /**
     * Return -1 if no more content to read or the offset of the expected TZ
     *
     * @param cal
     * @param str
     * @param off
     * @return
     * @throws ParseException
     */
    private final static int readMilliseconds(Calendar cal, String str, int off) throws ParseException {
        int e = str.indexOf('Z', off);
        if (e == -1) {
            e = str.indexOf('+', off);
            if (e == -1) {
                e = str.indexOf('-', off);
            }
        }
        String ms = e == -1 ? str.substring(off) : str.substring(off, e);
        // need to normalize the ms fraction to 3 digits.
        // If less than 3 digits right pad with 0
        // If more than 3 digits truncate to 3 digits.
        int mslen = ms.length();
        if (mslen > 0) {
            int f = 0;
            switch (mslen) {
            case 1:
                f = Integer.parseInt(ms) * 100;
                break;
            case 2:
                f = Integer.parseInt(ms) * 10;
                break;
            case 3:
                f = Integer.parseInt(ms);
                break;
            default: // truncate
                f = Integer.parseInt(ms.substring(0, 3));
                break;
            }
            cal.set(Calendar.MILLISECOND, f);
        }
        return e;
    }

    private static final boolean isChar(char c, String str, int off) {
        return str.length() > off && str.charAt(off) == c;
    }

    private static final int readCharOpt(char c, Calendar cal, String str, int off) {
        if (str.length() > off) {
            if (str.charAt(off) == c) {
                return off + 1;
            }
        }
        return -1;
    }

    private final static boolean readTimeZone(Calendar cal, String str, int off) throws ParseException {
        int len = str.length();
        if (len == off) {
            return false;
        }
        char c = str.charAt(off);
        if (c == 'Z') {
            return true;
        }
        off++;
        boolean plus = false;
        if (c == '+') {
            plus = true;
        } else if (c != '-') {
            throw new ParseException("Only Z, +, - prefixes are allowed in TZ", off);
        }
        int h = 0;
        int m = 0;
        int d = len - off;
        /**
         * We check here the different format of timezone * +02 (d=2 : doesn't seem to be in ISO-8601 but left for
         * compat) * +02:00 (d=5) * +0200 (d=4)
         */
        if (d == 2) {
            h = Integer.parseInt(str.substring(off, off + 2));
        } else if (d == 5) {
            h = Integer.parseInt(str.substring(off, off + 2));
            m = Integer.parseInt(str.substring(off + 3, off + 5));
            // we do not check for ':'. we assume it is in the correct format
        } else if (d == 4) {
            h = Integer.parseInt(str.substring(off, off + 2));
            m = Integer.parseInt(str.substring(off + 2, off + 4));
        } else {
            throw new ParseException("Invalid TZ in \"" + str + "\"", off);
        }

        if (plus) {
            cal.add(Calendar.HOUR, -h);
            cal.add(Calendar.MINUTE, -m);
        } else {
            cal.add(Calendar.HOUR, h);
            cal.add(Calendar.MINUTE, m);
        }

        return true;
    }

}
