/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core.query.sql.model;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * @author Florent Guillaume
 */
public class DateLiteral extends Literal {

    private static final long serialVersionUID = 279219479611055690L;

    public static final DateTimeFormatter dateParser = DateTimeFormatter.ISO_DATE.withLocale(Locale.getDefault());

    public static final DateTimeFormatter dateTimeParser = DateTimeFormatter.ISO_DATE_TIME;

    public static final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_DATE;

    public static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;

    // Direct access from org.nuxeo.ecm.core.search.backend.compass.QueryConverter
    public final ZonedDateTime value;

    public final boolean onlyDate;

    public DateLiteral(ZonedDateTime value) {
        this.value = value;
        this.onlyDate = false;
    }

    public DateLiteral(String value, boolean onlyDate) {
        this.onlyDate = onlyDate;
        if (onlyDate) {
            this.value = ZonedDateTime.parse(value, dateParser);
        } else {
            // workaround to allow space instead of T after the date part
            if (value.charAt(10) == ' ') {
                char[] s = value.toCharArray();
                s[10] = 'T';
                value = new String(s);
            }
            this.value = ZonedDateTime.parse(value, dateTimeParser);
        }
    }

    public Calendar toCalendar() {
        return GregorianCalendar.from(value);
    }

    public java.sql.Date toSqlDate() {
        return new java.sql.Date(value.toInstant().toEpochMilli());
    }

    @Override
    public String toString() {
        if (onlyDate) {
            String s = dateFormatter.format(value);
            return new StringBuffer(s.length() + 7).append("DATE '").append(s).append("'").toString();
        } else {
            String s = dateTimeFormatter.format(value);
            return new StringBuffer(s.length() + 12).append("TIMESTAMP '").append(s).append("'").toString();
        }
    }

    @Override
    public String asString() {
        if (onlyDate) {
            return dateFormatter.format(value);
        } else {
            return dateTimeFormatter.format(value);
        }
    }

    @Override
    public void accept(IVisitor visitor) {
        visitor.visitDateLiteral(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof DateLiteral) {
            return value.equals(((DateLiteral) obj).value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    public static String dateTime(DateLiteral date) {
        return dateTimeFormatter.format(date.value);
    }

    public static String date(DateLiteral date) {
        return dateFormatter.format(date.value);
    }
}
