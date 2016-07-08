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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

/**
 * @since 7.2
 */
public class MarshalingHelper {

    private MarshalingHelper() {
        // empty
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> unwrapMap(ScriptObjectMirror jso) {
        if (jso.isArray()) {
            throw new UnsupportedOperationException("JavaScript input is an Array!");
        }
        return (Map<String, Object>) unwrap(jso);
    }

    public static Object unwrap(ScriptObjectMirror jso) {
        if (jso.isArray()) {
            List<Object> l = new ArrayList<>();
            for (Object o : jso.values()) {
                if (o instanceof ScriptObjectMirror) {
                    l.add(unwrap((ScriptObjectMirror) o));
                } else {
                    l.add(o);
                }
            }
            return l;
        } else {
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
    }

    public static Object wrap(Map<String, Object> map) {
        return ScriptObjectMirror.wrap(map, null);
    }

}
