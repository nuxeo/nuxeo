/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
public class ScriptObjectMirrors {

    private static final String JAVASCRIPT_MAP_CLASS_TYPE = "Object";

    private static final String JAVASCRIPT_DATE_CLASS_TYPE = "Date";

    private static final String JAVASCRIPT_GLOBAL_CLASS_TYPE = "global";

    private static final String JAVASCRIPT_FUNCTION_CLASS_TYPE = "Function";


    private ScriptObjectMirrors() {
        // empty
    }

    public static Object unwrap(ScriptObjectMirror jso) {
        if (jso.isArray()) {
            return unwrapList(jso);
        } else if (JAVASCRIPT_MAP_CLASS_TYPE.equals(jso.getClassName())) {
            return unwrapMap(jso);
        } else if (JAVASCRIPT_DATE_CLASS_TYPE.equals(jso.getClassName())) {
            return unwrapDate(jso);
        } else if (JAVASCRIPT_GLOBAL_CLASS_TYPE.equals(jso.getClassName())) {
            return null;
        } else if (JAVASCRIPT_FUNCTION_CLASS_TYPE.equals(jso.getClassName())) {
            return null;
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
                result.put(k, DocumentScriptingWrapper.unwrap(o));
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
