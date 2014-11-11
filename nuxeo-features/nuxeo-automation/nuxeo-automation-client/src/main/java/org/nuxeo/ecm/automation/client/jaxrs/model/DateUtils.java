/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.jaxrs.model;

import java.util.Date;

/**
 * Parse and encode W3c dates. Only UTC dates are supported (ending in Z):
 * YYYY-MM-DDThh:mm:ssZ (without milliseconds) We use a custom parser since it
 * should work on GWT too.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@SuppressWarnings("deprecation")
public class DateUtils {
	
	// Utility class.
	private DateUtils() {
	}

    public static Date parseDate(String date) {
        int t = date.indexOf('T');
        Date result = new Date(0);
        if (t == -1) {
            fillDate(date, result);
        } else {
            fillDate(date.substring(0, t), result);
            fillTime(date.substring(t + 1), result);
        }
        return result;
    }

    public static String formatDate(Date date) {
        StringBuilder buf = new StringBuilder();
        int y = date.getYear() + 1900;
        int m = date.getMonth() + 1;
        int d = date.getDate();
        buf.append(y).append('-');
        if (m < 10) {
            buf.append("0");
        }
        buf.append(m).append('-');
        if (d < 10) {
            buf.append("0");
        }
        buf.append(d);
        buf.append('T');
        int h = date.getHours();
        m = date.getMinutes();
        int s = date.getSeconds();
        if (h < 10) {
            buf.append("0");
        }
        buf.append(h).append(':');
        if (m < 10) {
            buf.append("0");
        }
        buf.append(m).append(':');
        if (s < 10) {
            buf.append("0");
        }
        buf.append(s);
        buf.append('Z');
        return buf.toString();
    }

    protected static void fillDate(String text, Date date) {
        int p = text.indexOf('-');
        if (p == -1) {
            throw new IllegalArgumentException("Invalid date format: " + text);
        }
        String y = text.substring(0, p);
        int q = text.indexOf('-', p + 1);
        if (q == -1) {
            throw new IllegalArgumentException("Invalid date format: " + text);
        }
        String m = text.substring(p + 1, q);
        String d = text.substring(q + 1);
        int year = Integer.parseInt(y) - 1900;
        int month = Integer.parseInt(m) - 1;
        int day = Integer.parseInt(d);
        date.setYear(year);
        date.setMonth(month);
        date.setDate(day);
    }

    protected static void fillTime(String text, Date date) {
        int p = text.indexOf(':');
        if (p == -1) {
            throw new IllegalArgumentException("Invalid time format: " + text);
        }
        String h = text.substring(0, p);
        int q = text.indexOf(':', p + 1);
        if (q == -1) {
            throw new IllegalArgumentException("Invalid time format: " + text);
        }
        String m = text.substring(p + 1, q);
        String s = text.substring(q + 1, text.length() - 1); // remove the
                                                                // trailing Z
        int hour = Integer.parseInt(h);
        int minute = Integer.parseInt(m);
        int second = Integer.parseInt(s);
        date.setHours(hour);
        date.setMinutes(minute);
        date.setSeconds(second);
    }

    public static void main(String[] args) {
        Date d = new Date();
        String s = formatDate(d);
        System.out.println(s);
        Date d2 = parseDate(s);
        System.out.println(d + " = " + d2);
    }
}
