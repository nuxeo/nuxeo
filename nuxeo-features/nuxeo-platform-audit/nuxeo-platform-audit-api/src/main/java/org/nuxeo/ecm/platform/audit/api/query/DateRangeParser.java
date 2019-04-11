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
 */
public final class DateRangeParser {

    // Utility class.
    private DateRangeParser() {
    }

    public static Date parseDateRangeQuery(Date now, String dateRangeQuery) throws AuditQueryException {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(now);
            if (dateRangeQuery != null) {
                Map<String, Integer> parsed = parseQuery(dateRangeQuery);
                if (parsed.containsKey(DateRangeQueryConstants.HOUR)) {
                    calendar.add(Calendar.HOUR_OF_DAY, -parsed.get(DateRangeQueryConstants.HOUR));
                }
                if (parsed.containsKey(DateRangeQueryConstants.MIN)) {
                    calendar.add(Calendar.MINUTE, -parsed.get(DateRangeQueryConstants.MIN));
                }
            }
            return calendar.getTime();
        } catch (NumberFormatException nfe) {
            throw new AuditQueryException("Invalid query format...", nfe);
        }
    }

    private static Map<String, Integer> parseQuery(String query) throws AuditQueryException {
        Map<String, Integer> parsed = new HashMap<>();

        query = query.trim();
        query = query.replace(" ", "");

        int offsetMinutes = query.indexOf(DateRangeQueryConstants.MIN);
        int offsetHours = query.indexOf(DateRangeQueryConstants.HOUR);

        if (offsetMinutes != -1) {
            String sub = query.substring(0, offsetMinutes);
            try {
                parsed.put(DateRangeQueryConstants.MIN, Integer.parseInt(sub));
            } catch (NumberFormatException nfe) {
                throw new AuditQueryException(nfe.getMessage(), nfe);
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
                throw new AuditQueryException(nfe.getMessage(), nfe);
            }
        }
        return parsed;
    }

}
