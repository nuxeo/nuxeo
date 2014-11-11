/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 *
 */
public class RelationDate {

    public static final DateFormat ISO_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss");

    private RelationDate() {
    }

    /**
     * Returns Literal for given date.
     */
    public static Literal getLiteralDate(Date date) {
        return new LiteralImpl(ISO_FORMAT.format(date));
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
                date = ISO_FORMAT.parse(dateString);
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
