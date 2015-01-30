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
package org.nuxeo.automation.scripting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

/**
 * @since 7.2
 */
public class MarshalingHelper {

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
