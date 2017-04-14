/*
 * (C) Copyright 2016-2017 Nuxeo (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.api.model.Delta;

/**
 * @since 8.3
 */
final class MarkLogicHelper {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    public static final String DOCUMENT_ROOT = "document";

    public static final String DOCUMENT_ROOT_PATH = '/' + DOCUMENT_ROOT;

    public static final String ARRAY_ITEM_KEY_SUFFIX = "__item";

    public static final String ATTRIBUTE_TYPE = "type";

    public static final String ATTRIBUTE_XSI_TYPE = "xsi:" + ATTRIBUTE_TYPE;

    public static final char SCHEMA_ORIGINAL_DELIMITER = ':';

    public static final String SCHEMA_MARKLOGIC_DELIMITER = "__";

    public static String serializeCalendar(Calendar cal) {
        return DateTime.now().withMillis(cal.getTimeInMillis()).toString(DATE_TIME_FORMATTER);
    }

    public static Calendar deserializeCalendar(String calString) {
        DateTime dateTime = DATE_TIME_FORMATTER.parseDateTime(calString);
        Calendar cal = Calendar.getInstance();
        cal.setTime(dateTime.toDate());
        return cal;
    }

    public static String serializeKey(String nxKey) {
        // Do it this way to avoid memory issues by instantiating to many object to replace character
        StringBuilder mlKey = new StringBuilder(nxKey.length());
        for (int i = 0; i < nxKey.length(); i++) {
            char c = nxKey.charAt(i);
            if (c == SCHEMA_ORIGINAL_DELIMITER) {
                mlKey.append(SCHEMA_MARKLOGIC_DELIMITER);
            } else {
                mlKey.append(c);
            }
        }
        return mlKey.toString();
    }

    public static String deserializeKey(String mlKey) {
        // Do it this way to avoid memory issues by instantiating to many object to replace character
        StringBuilder nxKey = new StringBuilder(mlKey.length());
        boolean underscore = false;
        for (int i = 0; i < mlKey.length(); i++) {
            boolean underscoreNext = false;
            char c = mlKey.charAt(i);
            if (c == '_') {
                if (underscore) {
                    nxKey.append(SCHEMA_ORIGINAL_DELIMITER);
                } else {
                    underscoreNext = true;
                }
            } else {
                nxKey.append(c);
            }
            underscore = underscoreNext;
        }
        return nxKey.toString();
    }

    public static String getLastElementName(String path) {
        return path.replaceAll("^(.*/)?([^/]*)$", "$2");
    }

    public static String buildItemNameFromPath(String path) {
        return getLastElementName(path) + ARRAY_ITEM_KEY_SUFFIX;
    }

    public enum ElementType {

        BOOLEAN("xs:boolean", "boolean"),

        DOUBLE("xs:double", "double"),

        LONG("xs:long", "long"),

        CALENDAR("xs:dateTime", "dateTime"),

        STRING("xs:string", "string");

        private final String key;

        private final String keyWithoutNamespace;

        ElementType(String key, String keyWithoutNamespace) {
            this.key = key;
            this.keyWithoutNamespace = keyWithoutNamespace;
        }

        public String get() {
            return key;
        }

        public String getWithoutNamespace() {
            return keyWithoutNamespace;
        }

        public static ElementType of(String string) {
            String type = string.toLowerCase();
            for (ElementType elementType : values()) {
                if (elementType.get().equalsIgnoreCase(type)) {
                    return elementType;
                }
            }
            return null;
        }

        /**
         * @return the element type of simple type of input object.
         */
        public static ElementType getType(Object object) {
            return getType(object == null ? null : object.getClass());
        }

        /**
         * @return the element type of simple type of input class.
         */
        public static ElementType getType(Class<?> clazz) {
            if (Boolean.class.isAssignableFrom(clazz)) {
                return BOOLEAN;
            } else if (Double.class.isAssignableFrom(clazz)) {
                return DOUBLE;
            } else if (Long.class.isAssignableFrom(clazz) || Delta.class.isAssignableFrom(clazz)) {
                return LONG;
            } else if (Calendar.class.isAssignableFrom(clazz) || DateTime.class.isAssignableFrom(clazz)) {
                return CALENDAR;
            }
            return STRING;
        }

    }

}
