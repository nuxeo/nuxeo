/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.automation.client.model;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Parse / format ISO 8601 dates.
 * 
 * @author "Stephane Lacoin [aka matic] <slacoin at nuxeo.com>"
 * 
 */
public class DateParser {

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
        if (str == null) {
            return null;
        }
        try {
            return parse(str).getTime();
        } catch (ParseException e) {
            throw new IllegalArgumentException(
                    "Failed to parse ISO 8601 date: " + str, e);
        }
    }

    /**
     * 2011-10-23T12:00:00.00Z
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
        StringBuilder buf = new StringBuilder(32);
        return buf.append(cal.get(Calendar.YEAR)).append('-').append(
                pad(cal.get(Calendar.MONTH) + 1)).append('-').append(
                pad(cal.get(Calendar.DATE))).append('T').append(
                pad(cal.get(Calendar.HOUR_OF_DAY))).append(':').append(
                pad(cal.get(Calendar.MINUTE))).append(':').append(
                pad(cal.get(Calendar.SECOND))).append('Z').toString();
    }

    public static String formatW3CDateTimeMs(Date date) {
        if (date == null) {
            return null;
        }
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTime(date);
        StringBuilder buf = new StringBuilder(32);
        return buf.append(cal.get(Calendar.YEAR)).append('-').append(
                pad(cal.get(Calendar.MONTH) + 1)).append('-').append(
                pad(cal.get(Calendar.DATE))).append('T').append(
                pad(cal.get(Calendar.HOUR_OF_DAY))).append(':').append(
                pad(cal.get(Calendar.MINUTE))).append(':').append(
                pad(cal.get(Calendar.SECOND))).append('.').append(
                pad3(cal.get(Calendar.MILLISECOND))).append('Z').toString();
    }

    private final static String pad(int i) {
        return i < 10 ? "0".concat(String.valueOf(i)) : String.valueOf(i);
    }

    private final static String pad3(int i) {
        if (i < 10) {
            return "00".concat(String.valueOf(i));
        } else if (i < 100) {
            return "0".concat(String.valueOf(i));
        } else {
            return String.valueOf(i);
        }
    }

    private final static int readYear(Calendar cal, String str, int off)
            throws ParseException {
        if (str.length() >= off + 4) {
            cal.set(Calendar.YEAR,
                    Integer.parseInt(str.substring(off, off + 4)));
            return off + 4;
        }
        throw new ParseException("Invalid year in date '" + str + "'", off);
    }

    private final static int readMonth(Calendar cal, String str, int off)
            throws ParseException {
        if (str.length() >= off + 2) {
            cal.set(Calendar.MONTH,
                    Integer.parseInt(str.substring(off, off + 2)) - 1);
            return off + 2;
        }
        throw new ParseException("Invalid month in date '" + str + "'", off);
    }

    private final static int readDay(Calendar cal, String str, int off)
            throws ParseException {
        if (str.length() >= off + 2) {
            cal.set(Calendar.DATE,
                    Integer.parseInt(str.substring(off, off + 2)));
            return off + 2;
        }
        throw new ParseException("Invalid day in date '" + str + "'", off);
    }

    private final static int readHours(Calendar cal, String str, int off)
            throws ParseException {
        if (str.length() >= off + 2) {
            cal.set(Calendar.HOUR,
                    Integer.parseInt(str.substring(off, off + 2)));
            return off + 2;
        }
        throw new ParseException("Invalid hours in date '" + str + "'", off);
    }

    private final static int readMinutes(Calendar cal, String str, int off)
            throws ParseException {
        if (str.length() >= off + 2) {
            cal.set(Calendar.MINUTE,
                    Integer.parseInt(str.substring(off, off + 2)));
            return off + 2;
        }
        throw new ParseException("Invalid minutes in date '" + str + "'", off);
    }

    private final static int readSeconds(Calendar cal, String str, int off)
            throws ParseException {
        if (str.length() >= off + 2) {
            cal.set(Calendar.SECOND,
                    Integer.parseInt(str.substring(off, off + 2)));
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
    private final static int readMilliseconds(Calendar cal, String str, int off)
            throws ParseException {
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

    private static final int readCharOpt(char c, Calendar cal, String str,
            int off) {
        if (str.length() > off) {
            if (str.charAt(off) == c) {
                return off + 1;
            }
        }
        return -1;
    }

    private final static boolean readTimeZone(Calendar cal, String str, int off)
            throws ParseException {
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
            throw new ParseException("Only Z, +, - prefixes are allowed in TZ",
                    off);
        }
        int h = 0;
        int m = 0;
        int d = len - off;
        if (d == 2) {
            h = Integer.parseInt(str.substring(off, off + 2));
        } else if (d == 5) {
            h = Integer.parseInt(str.substring(off, off + 2));
            m = Integer.parseInt(str.substring(off + 3, off + 5));
            // we do not check for ':'. we assume it is in the correct format
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
