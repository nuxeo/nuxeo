/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id: RelationDate.java 28460 2008-01-03 15:34:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.api.impl;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.nuxeo.ecm.platform.relations.api.Literal;

/**
 * Relation date management.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class RelationDate {

    private RelationDate() {
    }

    protected static DateFormat getDateFormat() {
        // not thread-safe so don't use a static instance
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    }

    /**
     * Returns Literal for given date.
     */
    public static Literal getLiteralDate(Date date) {
        return new LiteralImpl(getDateFormat().format(date));
    }

    /**
     * @return Literal for given Calendar instance
     */
    public static Literal getLiteralDate(Calendar cal) {
        return getLiteralDate(cal.getTime());
    }

    /**
     * @return Date instance for given literal.
     */
    public static Date getDate(Literal dateLiteral) {
        Date date = null;
        if (dateLiteral != null) {
            String dateString = dateLiteral.getValue();
            try {
                date = getDateFormat().parse(dateString);
            } catch (ParseException err) {
            }
        }
        return date;
    }

    /**
     * @return Calendar instance for given literal.
     */
    public static Calendar getCalendar(Literal dateLiteral) {
        // TODO optim ?
        Calendar cal = Calendar.getInstance();
        Date date = getDate(dateLiteral);
        if (date == null) {
            return null;
        }

        cal.setTime(date);
        return cal;
    }

}
