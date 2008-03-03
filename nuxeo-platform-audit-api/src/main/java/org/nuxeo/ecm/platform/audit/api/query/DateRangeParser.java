/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     anguenot
 *
 * $Id: DateRangeParser.java 20577 2007-06-16 09:26:07Z sfermigier $
 */

package org.nuxeo.ecm.platform.audit.api.query;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Date range parser.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public final class DateRangeParser {

    // Utility class.
    private DateRangeParser() {
    }

    public static Date parseDateRangeQuery(Date now, String dateRangeQuery)
            throws AuditQueryException {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(now);
            if (dateRangeQuery != null) {
                Map<String, Integer> parsed = parseQuery(dateRangeQuery);
                if (parsed.containsKey(DateRangeQueryConstants.HOUR)) {
                    calendar.add(Calendar.HOUR_OF_DAY,
                            -parsed.get(DateRangeQueryConstants.HOUR));
                }
                if (parsed.containsKey(DateRangeQueryConstants.MIN)) {
                    calendar.add(Calendar.MINUTE,
                            -parsed.get(DateRangeQueryConstants.MIN));

                }
            }
            return calendar.getTime();
        } catch (NumberFormatException nfe) {
            throw new AuditQueryException("Invalid query format...", nfe);
        }
    }

    private static Map<String, Integer> parseQuery(String query)
            throws AuditQueryException {
        Map<String, Integer> parsed = new HashMap<String, Integer>();

        query = query.trim();
        query = query.replace(" ", "");

        int offsetMinutes = query.indexOf(DateRangeQueryConstants.MIN);
        int offsetHours = query.indexOf(DateRangeQueryConstants.HOUR);

        if (offsetMinutes != -1) {
            String sub = query.substring(0, offsetMinutes);
            try {
                parsed.put(DateRangeQueryConstants.MIN, Integer.parseInt(sub));
            } catch (NumberFormatException nfe) {
                throw new AuditQueryException(nfe.getMessage());
            }
        }

        if (offsetHours != -1) {
            String sub;
            if (offsetMinutes == -1) {
                sub = query.substring(0, offsetHours);
            } else {
                sub = query.substring(offsetMinutes + 1, offsetHours);
            }
            try {
                parsed.put(DateRangeQueryConstants.HOUR, Integer.parseInt(sub));
            } catch (NumberFormatException nfe) {
                throw new AuditQueryException(nfe.getMessage());
            }
        }

        return parsed;
    }

}
