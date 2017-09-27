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

import java.util.Calendar;
import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * @author Florent Guillaume
 */
public class DateLiteral extends Literal {

    private static final long serialVersionUID = 279219479611055690L;

    public static final DateTimeFormatter dateParser = ISODateTimeFormat.dateParser().withLocale(Locale.getDefault());

    public static final DateTimeFormatter dateTimeParser = ISODateTimeFormat.dateOptionalTimeParser().withOffsetParsed();

    public static final DateTimeFormatter dateFormatter = ISODateTimeFormat.date();

    public static final DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime();

    // Direct access from org.nuxeo.ecm.core.search.backend.compass.QueryConverter
    public final DateTime value;

    public final boolean onlyDate;

    public DateLiteral(DateTime value) {
        this.value = value;
        this.onlyDate = false;
    }

    public DateLiteral(String value, boolean onlyDate) {
        this.onlyDate = onlyDate;
        if (onlyDate) {
            this.value = dateParser.parseDateTime(value);
        } else {
            // workaround to allow space instead of T after the date part
            if (value.charAt(10) == ' ') {
                char[] s = value.toCharArray();
                s[10] = 'T';
                value = new String(s);
            }
            this.value = dateTimeParser.parseDateTime(value);
        }
    }

    public Calendar toCalendar() {
        return value.toGregorianCalendar();
    }

    public java.sql.Date toSqlDate() {
        return new java.sql.Date(value.toDate().getTime());
    }

    @Override
    public String toString() {
        if (onlyDate) {
            String s = dateFormatter.print(value);
            return new StringBuffer(s.length() + 7).append("DATE '").append(s).append("'").toString();
        } else {
            String s = dateTimeFormatter.print(value);
            return new StringBuffer(s.length() + 12).append("TIMESTAMP '").append(s).append("'").toString();
        }
    }

    @Override
    public String asString() {
        if (onlyDate) {
            return dateFormatter.print(value);
        } else {
            return dateTimeFormatter.print(value);
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
        return dateTimeFormatter.print(date.value);
    }

    public static String date(DateLiteral date) {
        return dateFormatter.print(date.value);
    }
}
