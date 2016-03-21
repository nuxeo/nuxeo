/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.storage.marklogic;

import java.util.Calendar;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * @since 8.3
 */
final class MarkLogicHelper {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    public static final String DOCUMENT_ROOT = "document";

    public static final String ARRAY_ITEM_NAMESPACE = "ml";

    public static final String ARRAY_ITEM_KEY = ARRAY_ITEM_NAMESPACE + ":item";

    public static final String ATTRIBUTE_TYPE = "type";

    public static final String ATTRIBUTE_XSI_TYPE = "xsi:" + ATTRIBUTE_TYPE;

    public static String serializeCalendar(Calendar cal) {
        return DateTime.now().withMillis(cal.getTimeInMillis()).toString(DATE_TIME_FORMATTER);
    }

    public static Calendar deserializeCalendar(String calString) {
        DateTime dateTime = DATE_TIME_FORMATTER.parseDateTime(calString);
        Calendar cal = Calendar.getInstance();
        cal.setTime(dateTime.toDate());
        return cal;
    }

    public enum ElementType {

        BOOLEAN("xs:boolean"),

        LONG("xs:long"),

        CALENDAR("xs:dateTime"),

        STRING("xs:string");

        private String key;

        ElementType(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public static ElementType of(String string) {
            String type = string.toLowerCase();
            for (ElementType elementType : values()) {
                if (elementType.getKey().equalsIgnoreCase(type)) {
                    return elementType;
                }
            }
            return null;
        }

        /**
         * @return the element type of simple type of input class.
         */
        public static ElementType getType(Class<?> clazz) {
            if (Boolean.class.isAssignableFrom(clazz)) {
                return BOOLEAN;
            } else if (Long.class.isAssignableFrom(clazz)) {
                return LONG;
            } else if (Calendar.class.isAssignableFrom(clazz)) {
                return CALENDAR;
            }
            return STRING;
        }

    }

}
