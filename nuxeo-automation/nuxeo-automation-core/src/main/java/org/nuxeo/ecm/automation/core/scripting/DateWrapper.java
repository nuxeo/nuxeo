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
 */
public class DateWrapper {

    protected final Calendar date;

    public DateWrapper() {
        this.date = Calendar.getInstance();
    }

    public DateWrapper(Calendar date) {
        this.date = date;
    }

    public DateWrapper(Date date) {
        this.date = Calendar.getInstance();
        this.date.setTime(date);
    }

    public DateWrapper(long date) {
        this.date = Calendar.getInstance();
        this.date.setTimeInMillis(date);
    }

    public DateWrapper months(int months) {
        date.add(Calendar.MONTH, months);
        return this;
    }

    public DateWrapper days(int days) {
        date.add(Calendar.DAY_OF_MONTH, days);
        return this;
    }

    public DateWrapper years(int years) {
        date.add(Calendar.YEAR, years);
        return this;
    }

    public DateWrapper seconds(int seconds) {
        date.add(Calendar.SECOND, seconds);
        return this;
    }

    public DateWrapper weeks(int weeks) {
        date.add(Calendar.WEEK_OF_MONTH, weeks);
        return this;
    }

    public Calendar getCalendar() {
        return date;
    }

    public Date getDate() {
        return date.getTime();
    }

    public long getTime() {
        return date.getTimeInMillis();
    }

    public int getYear() {
        return date.get(Calendar.YEAR);
    }

    public int getMonth() {
        return date.get(Calendar.MONTH);
    }

    public int getDay() {
        return date.get(Calendar.DAY_OF_MONTH);
    }

    public int getMinute() {
        return date.get(Calendar.MINUTE);
    }

    public int getHour() {
        return date.get(Calendar.HOUR);
    }

    public int getSecond() {
        return date.get(Calendar.SECOND);
    }

    public int getWeek() {
        return date.get(Calendar.WEEK_OF_YEAR);
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
