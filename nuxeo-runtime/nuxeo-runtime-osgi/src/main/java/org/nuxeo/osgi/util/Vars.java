/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.osgi.util;

import java.util.HashMap;
import java.util.Map;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Vars {

    public static interface Resolver {
        String get(String key);
    }

    public static Map<String,String> expand(final Map<String,String> vars) {
        return expand(vars, null);
    }

    public static Map<String,String> expand(final Map<String,String> vars, final Resolver resolver) {
        final Map<String,String> result = new HashMap<String, String>(vars.size());
        Resolver resolver2 = new Resolver() {
            @Override
            public String get(String key) {
                String v = result.get(key);
                if (v == null) {
                    v = vars.get(key);
                    if (v == null && resolver != null) {
                        return resolver.get(key);
                    }
                }
                return v;
            }
        };
        for (Map.Entry<?,?> entry : vars.entrySet()) {
            String key = (String)entry.getKey();
            String v = (String)entry.getValue();
            if (v == null) {
                result.put(key, null);
            } else {
                String rv = expand(v, resolver2);
                while (!rv.equals(v)) {
                    v = rv;
                    rv = expand(v, resolver2);
                }
                result.put(key, rv);
            }
        }
        return result;
    }

    public static String expand(String expression, final Map<?,?> vars) {
        int s = expression.indexOf("${", 0);
        if (s == -1) {
            return expression;
        }
        int e = expression.indexOf('}', s+2);
        if (e == -1) {
            return expression;
        }
        return expand(expression, 0, s, e, new Resolver() {
            @Override
            public String get(String key) {
                Object v = vars.get(key);
                return v != null ? v.toString() : null;
            }
        });
    }

    public static String expand(String expression, Resolver resolver) {
        int s = expression.indexOf("${", 0);
        if (s == -1) {
            return expression;
        }
        int e = expression.indexOf('}', s+2);
        if (e == -1) {
            return expression;
        }
        return expand(expression, 0, s, e, resolver);
    }

    private static String expand(String expression, int offset, int s, int e, Resolver resolver) {
        StringBuilder buf = new StringBuilder();

        do {
            if (s > offset) {
                buf.append(expression.substring(offset, s));
            }
            String v = resolveVar(expression.substring(s+2, e), resolver);
            if (v == null) {
                buf.append(expression.substring(s,e+1));
            } else {
                buf.append(v);
            }
            offset = e + 1;
            s = expression.indexOf("${", offset);
            if (s == -1) {
                break;
            }
            e = expression.indexOf('}', s+2);
            if (e == -1) {
                break;
            }
        } while (true);

        if (offset < expression.length()) {
            buf.append(expression.substring(offset));
        }

        return buf.toString();
    }

    private final static String resolveVar(String var, Resolver resolver) {
        String key = var;
        int i = var.indexOf('?');
        if (i > -1) {
            key = key.substring(0, i);
            Object v = resolver.get(key);
            if (v != null) {
                return v.toString();
            } else {
                return var.substring(i+1);
            }
        }
        Object v = resolver.get(key);
        return v != null ? v.toString() : null;
    }

}
