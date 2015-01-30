package org.nuxeo.automation.scripting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

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
                    l.add(unwrap((ScriptObjectMirror)o));
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
                    result.put(k, unwrap((ScriptObjectMirror)o));
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
