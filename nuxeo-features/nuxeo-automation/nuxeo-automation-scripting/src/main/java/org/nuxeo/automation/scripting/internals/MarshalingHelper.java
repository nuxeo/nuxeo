/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat <tdelprat@nuxeo.com>
 */
package org.nuxeo.automation.scripting.internals;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

/**
 * @since 7.2
 */
public class MarshalingHelper {

    private static final String JAVASCRIPT_MAP_CLASS_TYPE = "Object";

    private static final String JAVASCRIPT_DATE_CLASS_TYPE = "Date";

    public static Object unwrap(ScriptObjectMirror jso) {
        if (jso.isArray()) {
            return unwrapList(jso);
        } else if (JAVASCRIPT_MAP_CLASS_TYPE.equals(jso.getClassName())) {
            return unwrapMap(jso);
        } else if (JAVASCRIPT_DATE_CLASS_TYPE.equals(jso.getClassName())) {
            return unwrapDate(jso);
        } else {
            throw new UnsupportedOperationException(jso.getClassName() + " is not supported!");
        }
    }

    /**
     * @since 8.4
     */
    public static List<Object> unwrapList(ScriptObjectMirror jso) {
        if (!jso.isArray()) {
            throw new IllegalArgumentException("JavaScript input is not an Array!");
        }
        List<Object> l = new ArrayList<>();
        for (Object o : jso.values()) {
            if (o instanceof ScriptObjectMirror) {
                l.add(unwrap((ScriptObjectMirror) o));
            } else {
                l.add(o);
            }
        }
        return l;
    }

    public static Map<String, Object> unwrapMap(ScriptObjectMirror jso) {
        if (!JAVASCRIPT_MAP_CLASS_TYPE.equals(jso.getClassName())) {
            throw new IllegalArgumentException("JavaScript input is not an Object!");
        }
        Map<String, Object> result = new HashMap<>();
        for (String k : jso.keySet()) {
            Object o = jso.get(k);
            if (o instanceof ScriptObjectMirror) {
                result.put(k, unwrap((ScriptObjectMirror) o));
            } else {
                result.put(k, o);
            }
        }
        return result;
    }

    /**
     * @since 8.4
     */
    public static Calendar unwrapDate(ScriptObjectMirror jso) {
        if (!JAVASCRIPT_DATE_CLASS_TYPE.equals(jso.getClassName())) {
            throw new IllegalArgumentException("JavaScript input is not a Date!");
        }
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(((Double) jso.callMember("getTime")).longValue());
        return cal;
    }

    public static Object wrap(Map<String, Object> map) {
        return ScriptObjectMirror.wrap(map, null);
    }

}
