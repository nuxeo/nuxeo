/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Abstraction for a Map<String, Serializable> that is Serializable.
 *
 * @since 5.9.5
 */
public class State extends HashMap<String, Serializable> {

    private static final long serialVersionUID = 1L;

    private static final int DEBUG_MAX_STRING = 100;

    private static final int DEBUG_MAX_ARRAY = 10;

    private static final Set<String> KEY_ORDER = new LinkedHashSet<>(
            Arrays.asList(new String[] { "ecm:id", "ecm:primaryType",
                    "ecm:name", "ecm:parentId", "ecm:isVersion", "ecm:isProxy" }));

    /** Empty constructor. */
    public State() {
        super();
    }

    /** Copy constructor. */
    public State(State state) {
        super(state);
    }

    private State(Map<String, Serializable> map) {
        super(map);
    }

    public static State singleton(String key, Serializable value) {
        return new State(Collections.singletonMap(key, value));
    }

    /**
     * Overridden to display Calendars and arrays better, and truncate long
     * strings and arrays.
     * <p>
     * Also displays some keys first (ecm:id, ecm:name, ecm:primaryType)
     */
    @Override
    public String toString() {
        if (isEmpty()) {
            return "{}";
        }
        StringBuilder buf = new StringBuilder();
        buf.append('{');
        boolean empty = true;
        // some keys go first
        for (String key : KEY_ORDER) {
            if (containsKey(key)) {
                if (!empty) {
                    buf.append(", ");
                }
                empty = false;
                buf.append(key);
                buf.append('=');
                toString(buf, get(key));
            }
        }
        // sort keys
        String[] keys = keySet().toArray(new String[0]);
        Arrays.sort(keys);
        for (String key : keys) {
            if (KEY_ORDER.contains(key)) {
                // already done
                continue;
            }
            if (!empty) {
                buf.append(", ");
            }
            empty = false;
            buf.append(key);
            buf.append('=');
            toString(buf, get(key));
        }
        buf.append('}');
        return buf.toString();
    }

    @SuppressWarnings("boxing")
    protected static void toString(StringBuilder buf, Object value) {
        if (value instanceof String) {
            String v = (String) value;
            if (v.length() > DEBUG_MAX_STRING) {
                v = v.substring(0, DEBUG_MAX_STRING) + "...(" + v.length()
                        + " chars)...";
            }
            buf.append(v);
        } else if (value instanceof Calendar) {
            Calendar cal = (Calendar) value;
            char sign;
            int offset = cal.getTimeZone().getOffset(cal.getTimeInMillis()) / 60000;
            if (offset < 0) {
                offset = -offset;
                sign = '-';
            } else {
                sign = '+';
            }
            buf.append(String.format(
                    "Calendar(%04d-%02d-%02dT%02d:%02d:%02d.%03d%c%02d:%02d)",
                    cal.get(Calendar.YEAR), //
                    cal.get(Calendar.MONTH) + 1, //
                    cal.get(Calendar.DAY_OF_MONTH), //
                    cal.get(Calendar.HOUR_OF_DAY), //
                    cal.get(Calendar.MINUTE), //
                    cal.get(Calendar.SECOND), //
                    cal.get(Calendar.MILLISECOND), //
                    sign, offset / 60, offset % 60));
        } else if (value instanceof Object[]) {
            Object[] v = (Object[]) value;
            buf.append('[');
            for (int i = 0; i < v.length; i++) {
                if (i > 0) {
                    buf.append(',');
                    if (i > DEBUG_MAX_ARRAY) {
                        buf.append("...(" + v.length + " items)...");
                        break;
                    }
                }
                toString(buf, v[i]);
            }
            buf.append(']');
        } else {
            buf.append(value);
        }
    }

}
