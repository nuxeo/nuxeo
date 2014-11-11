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
package org.nuxeo.ecm.automation.core.scripting;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:tm@nuxeo.com">Thierry Martins</a>
 */
public class DateWrapper {

    protected final long timestamp;

    protected final Calendar date;

    public DateWrapper() {
        this.date = Calendar.getInstance();
        timestamp = date.getTimeInMillis();
    }

    public DateWrapper(Calendar date) {
        this.date = date;
        timestamp = date.getTimeInMillis();
    }

    public DateWrapper(Date date) {
        this(date.getTime());
    }

    public DateWrapper(long date) {
        timestamp = date;
        this.date = Calendar.getInstance();
        this.date.setTimeInMillis(timestamp);
    }

    public DateWrapper months(int months) {
        return dateWrapper(Calendar.MONTH, months);
    }

    public DateWrapper days(int days) {
        return dateWrapper(Calendar.DAY_OF_MONTH, days);
    }

    public DateWrapper years(int years) {
        return dateWrapper(Calendar.YEAR, years);
    }

    /**
     * @since 5.7
     */
    protected DateWrapper dateWrapper(int unit, int value) {
        Calendar calendar = (Calendar)this.date.clone();
        calendar.add(unit, value);
        return new DateWrapper(calendar);
    }

    public DateWrapper seconds(int seconds) {
        return dateWrapper(Calendar.SECOND, seconds);
    }

    public DateWrapper weeks(int weeks) {
        return dateWrapper(Calendar.WEEK_OF_MONTH, weeks);
    }

    public Calendar getCalendar() {
        return date;
    }

    public Date getDate() {
        return new Date(timestamp);
    }

    public long getTime() {
        return timestamp;
    }

    public int getYear() {
        return getCalendar().get(Calendar.YEAR);
    }

    public int getMonth() {
        return getCalendar().get(Calendar.MONTH);
    }

    public int getDay() {
        return getCalendar().get(Calendar.DAY_OF_MONTH);
    }

    public int getMinute() {
        return getCalendar().get(Calendar.MINUTE);
    }

    public int getHour() {
        return getCalendar().get(Calendar.HOUR);
    }

    public int getSecond() {
        return getCalendar().get(Calendar.SECOND);
    }

    public int getWeek() {
        return getCalendar().get(Calendar.WEEK_OF_YEAR);
    }

    public String format(String format) {
        return new SimpleDateFormat(format).format(date.getTime());
    }

    @Override
    public String toString() {
        return toQueryString();
    }

    public String toQueryString() {
        return new SimpleDateFormat("'TIMESTAMP' ''yyyy-MM-dd HH:mm:ss.SSS''").format(getDate());
    }

    public static void main(String[] args) {
        DateWrapper d = new DateWrapper();
        System.out.println(d);
        d.months(2);
        System.out.println(d);
        d.months(-2);
        System.out.println(d);
        d.months(-2);
        System.out.println(d);
        d.months(-1);
        System.out.println(d);

        d.years(-2);
        System.out.println(d);

        d.years(4);
        System.out.println(d);

        d.weeks(2);
        System.out.println(d);

        d.weeks(-1);
        System.out.println(d);

    }
}