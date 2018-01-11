/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.common.utils;

import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Yet Another variable resolver.
 *
 * @see TextTemplate
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Vars {

    public static String expand(String expression, final Map<?, ?> vars) {
        int s = expression.indexOf("${", 0);
        if (s == -1) {
            return expression;
        }
        int e = expression.indexOf('}', s + 2);
        if (e == -1) {
            return expression;
        }
        return expand(expression, 0, s, e, key -> {
            Object v = vars.get(key);
            return v != null ? v.toString() : null;
        });
    }

    private static String expand(String expression, int offset, int s, int e, UnaryOperator<String> resolver) {
        StringBuilder buf = new StringBuilder();

        do {
            if (s > offset) {
                buf.append(expression.substring(offset, s));
            }
            String v = resolveVar(expression.substring(s + 2, e), resolver);
            if (v == null) {
                buf.append(expression.substring(s, e + 1));
            } else {
                buf.append(v);
            }
            offset = e + 1;
            s = expression.indexOf("${", offset);
            if (s == -1) {
                break;
            }
            e = expression.indexOf('}', s + 2);
            if (e == -1) {
                break;
            }
        } while (true);

        if (offset < expression.length()) {
            buf.append(expression.substring(offset));
        }

        return buf.toString();
    }

    private static String resolveVar(String var, UnaryOperator<String> resolver) {
        String key = var;
        int i = var.indexOf('?');
        if (i > -1) {
            key = key.substring(0, i);
            Object v = resolver.apply(key);
            if (v != null) {
                return v.toString();
            } else {
                return var.substring(i + 1);
            }
        }
        Object v = resolver.apply(key);
        return v != null ? v.toString() : null;
    }

}
