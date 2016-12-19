/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.sql.jdbc;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.model.Delta;
import org.nuxeo.ecm.core.storage.sql.Row;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;

/**
 * Logger used for debugging.
 */
public class JDBCLogger {

    public static final Log log = LogFactory.getLog(JDBCLogger.class);

    public static final int DEBUG_MAX_STRING = 100;

    public static final int DEBUG_MAX_ARRAY = 10;

    public final String instance;

    public JDBCLogger(String instance) {
        this.instance = instance;
    }

    public boolean isLogEnabled() {
        return log.isTraceEnabled();
    }

    public String formatMessage(String message) {
        return "(" + instance + ") SQL: " + message;
    }

    public void error(String message) {
        log.error(formatMessage(message));
    }

    public void error(String message, Throwable t) {
        log.error(formatMessage(message), t);
    }

    public void warn(String message) {
        log.warn(formatMessage(message));
    }

    public void info(String message) {
        log.info(formatMessage(message));
    }

    public void log(String message) {
        log.trace(formatMessage(message));
    }

    public void logCount(int count) {
        if (count > 0 && isLogEnabled()) {
            log("  -> " + count + " row" + (count > 1 ? "s" : ""));
        }
    }

    public void logCounts(int[] counts) {
        if (!isLogEnabled()) {
            return;
        }
        int count = 0;
        for (int c : counts) {
            count += c;
        }
        logCount(count);
    }

    public void logResultSet(ResultSet rs, List<Column> columns) throws SQLException {
        List<String> res = new LinkedList<>();
        int i = 0;
        for (Column column : columns) {
            i++;
            Serializable v = column.getFromResultSet(rs, i);
            res.add(column.getKey() + "=" + loggedValue(v));
        }
        log("  -> " + String.join(", ", res));
    }

    public void logMap(Map<String, Serializable> map) throws SQLException {
        String result = map.entrySet()
                           .stream()
                           .map(entry -> entry.getKey() + "=" + loggedValue(entry.getValue()))
                           .collect(Collectors.joining(", "));
        log("  -> " + result);
    }

    public void logIds(List<Serializable> ids, boolean countTotal, long totalSize) {
        List<Serializable> debugIds = ids;
        String end = "";
        if (ids.size() > DEBUG_MAX_ARRAY) {
            debugIds = new ArrayList<>(DEBUG_MAX_ARRAY);
            int i = 0;
            for (Serializable id : ids) {
                debugIds.add(id);
                i++;
                if (i == DEBUG_MAX_ARRAY) {
                    break;
                }
            }
            end = "...(" + ids.size() + " ids)...";
        }
        if (countTotal) {
            end += " (total " + totalSize + ')';
        }
        log("  -> " + debugIds + end);
    }

    public void logSQL(String sql, List<Column> columns, Row row) {
        logSQL(sql, columns, row, Collections.<String> emptySet());
    }

    public void logSQL(String sql, List<Column> columns, Row row, Set<String> deltas) {
        List<Serializable> values = new ArrayList<>(columns.size());
        for (Column column : columns) {
            String key = column.getKey();
            Serializable value = row.get(key);
            if (deltas.contains(key)) {
                value = ((Delta) value).getDeltaValue();
            }
            values.add(value);
        }
        logSQL(sql, values);
    }

    // callable statement with one return value
    private static final String CALLABLE_START = "{?=";

    public void logSQL(String sql, Collection<Serializable> values) {
        StringBuilder buf = new StringBuilder();
        int start = 0;
        if (sql.startsWith(CALLABLE_START)) {
            buf.append(CALLABLE_START);
            start = CALLABLE_START.length();
        }
        for (Serializable v : values) {
            int index = sql.indexOf('?', start);
            if (index == -1) {
                // mismatch between number of ? and number of values
                break;
            }
            buf.append(sql, start, index);
            buf.append(loggedValue(v));
            start = index + 1;
        }
        buf.append(sql, start, sql.length());
        log(buf.toString());
    }

    /**
     * Returns a loggable value using pseudo-SQL syntax.
     */
    @SuppressWarnings("boxing")
    public static String loggedValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof String) {
            String v = (String) value;
            if (v.length() > DEBUG_MAX_STRING) {
                v = v.substring(0, DEBUG_MAX_STRING) + "...(" + v.length() + " chars)...";
            }
            return "'" + v.replace("'", "''") + "'";
        }
        if (value instanceof Calendar) {
            Calendar cal = (Calendar) value;
            char sign;
            int offset = cal.getTimeZone().getOffset(cal.getTimeInMillis()) / 60000;
            if (offset < 0) {
                offset = -offset;
                sign = '-';
            } else {
                sign = '+';
            }
            return String.format("TIMESTAMP '%04d-%02d-%02dT%02d:%02d:%02d.%03d%c%02d:%02d'", cal.get(Calendar.YEAR), //
                    cal.get(Calendar.MONTH) + 1, //
                    cal.get(Calendar.DAY_OF_MONTH), //
                    cal.get(Calendar.HOUR_OF_DAY), //
                    cal.get(Calendar.MINUTE), //
                    cal.get(Calendar.SECOND), //
                    cal.get(Calendar.MILLISECOND), //
                    sign, offset / 60, offset % 60);
        }
        if (value instanceof java.sql.Date) {
            return "DATE '" + value.toString() + "'";
        }
        if (value instanceof Object[]) {
            Object[] v = (Object[]) value;
            StringBuilder b = new StringBuilder();
            b.append('[');
            for (int i = 0; i < v.length; i++) {
                if (i > 0) {
                    b.append(',');
                    if (i > DEBUG_MAX_ARRAY) {
                        b.append("...(").append(v.length).append(" items)...");
                        break;
                    }
                }
                b.append(loggedValue(v[i]));
            }
            b.append(']');
            return b.toString();
        }
        return value.toString();
    }
}
