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
 *     Stephane Lacoin
 */
package org.nuxeo.apidoc.introspection;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contains code from Abdera to format dates.
 */
public class DateTimeFormat {

    /**
     * Create the serialized string form from a java.util.Date
     */
    public static String format(Date date) {
        return abderaFormat(date);
    }

    /**
     * Parse the serialized string form into a java.util.Date
     *
     * @param value
     *            The serialized string form of the date
     * @return The created java.util.Date
     */
    public static Date parse(String value) {
        SimpleDateFormat formatter = new SimpleDateFormat(value.endsWith("Z") ? "yyyyMMdd'T'HHmmss'Z'"
                : "yyyyMMdd'T'HHmmssZ");
        try {
            return formatter.parse(value);
        } catch (ParseException e) {
            return abderaParse(value);
        }
    }

    private static final Pattern PATTERN = Pattern.compile(
            "(\\d{4})(?:-(\\d{2}))?(?:-(\\d{2}))?(?:[Tt](?:(\\d{2}))?(?::(\\d{2}))?(?::(\\d{2}))?(?:\\.(\\d{3}))?)?([Zz])?(?:([+-])(\\d{2}):(\\d{2}))?");

    public static String abderaFormat(Date date) {
        StringBuilder sb = new StringBuilder();
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.setTime(date);
        sb.append(c.get(Calendar.YEAR));
        sb.append('-');
        int f = c.get(Calendar.MONTH);
        if (f < 9) {
            sb.append('0');
        }
        sb.append(f + 1);
        sb.append('-');
        f = c.get(Calendar.DATE);
        if (f < 10) {
            sb.append('0');
        }
        sb.append(f);
        sb.append('T');
        f = c.get(Calendar.HOUR_OF_DAY);
        if (f < 10) {
            sb.append('0');
        }
        sb.append(f);
        sb.append(':');
        f = c.get(Calendar.MINUTE);
        if (f < 10) {
            sb.append('0');
        }
        sb.append(f);
        sb.append(':');
        f = c.get(Calendar.SECOND);
        if (f < 10) {
            sb.append('0');
        }
        sb.append(f);
        sb.append('.');
        f = c.get(Calendar.MILLISECOND);
        if (f < 100) {
            sb.append('0');
        }
        if (f < 10) {
            sb.append('0');
        }
        sb.append(f);
        sb.append('Z');
        return sb.toString();
    }

    public static Date abderaParse(String value) {
        Matcher m = PATTERN.matcher(value);
        if (m.find()) {
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            int hoff = 0, moff = 0, doff = -1;
            if (m.group(9) != null) {
                doff = m.group(9).equals("-") ? 1 : -1;
                hoff = doff * (m.group(10) != null ? Integer.parseInt(m.group(10)) : 0);
                moff = doff * (m.group(11) != null ? Integer.parseInt(m.group(11)) : 0);
            }
            c.set(Calendar.YEAR, Integer.parseInt(m.group(1)));
            c.set(Calendar.MONTH, m.group(2) != null ? Integer.parseInt(m.group(2)) - 1 : 0);
            c.set(Calendar.DATE, m.group(3) != null ? Integer.parseInt(m.group(3)) : 1);
            c.set(Calendar.HOUR_OF_DAY, m.group(4) != null ? Integer.parseInt(m.group(4)) + hoff : 0);
            c.set(Calendar.MINUTE, m.group(5) != null ? Integer.parseInt(m.group(5)) + moff : 0);
            c.set(Calendar.SECOND, m.group(6) != null ? Integer.parseInt(m.group(6)) : 0);
            c.set(Calendar.MILLISECOND, m.group(7) != null ? Integer.parseInt(m.group(7)) : 0);
            return c.getTime();
        } else {
            throw new IllegalArgumentException("Invalid Date Format");
        }
    }

}
