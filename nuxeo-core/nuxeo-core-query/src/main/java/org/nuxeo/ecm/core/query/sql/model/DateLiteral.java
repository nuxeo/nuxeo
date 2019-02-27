/*
 * (C) Copyright 2006-2019 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.nuxeo.common.utils.DateUtils.formatISODateTime;
import static org.nuxeo.common.utils.DateUtils.parseISODateTime;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * @author Florent Guillaume
 */
public class DateLiteral extends Literal {

    private static final long serialVersionUID = 279219479611055690L;

    public final ZonedDateTime value;

    public final boolean onlyDate;

    public DateLiteral(ZonedDateTime value) {
        this.value = value;
        this.onlyDate = false;
    }

    public DateLiteral(String value, boolean onlyDate) {
        this.onlyDate = onlyDate;
        this.value = parseISODateTime(value);
    }

    public Calendar toCalendar() {
        return GregorianCalendar.from(value);
    }

    public java.sql.Date toSqlDate() {
        return new java.sql.Date(value.toInstant().toEpochMilli());
    }

    @Override
    public String toString() {
        String s = formatISODateTime(value, onlyDate);
        if (onlyDate) {
            return "DATE '" + s + "'";
        } else {
            return "TIMESTAMP '" + s + "'";
        }
    }

    @Override
    public String asString() {
        return formatISODateTime(value, onlyDate);
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

    /**
     * @deprecated since 11.1 as not used
     */
    @Deprecated
    public static String dateTime(DateLiteral date) {
        return formatISODateTime(date.value);
    }

    /**
     * @deprecated since 11.1 as not used
     */
    @Deprecated
    public static String date(DateLiteral date) {
        return formatISODateTime(date.value);
    }
}
