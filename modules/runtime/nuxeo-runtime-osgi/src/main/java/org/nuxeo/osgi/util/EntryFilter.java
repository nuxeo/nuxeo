/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.ArrayList;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class EntryFilter {

    public static final EntryFilter ANY = new EntryFilter() {
        @Override
        public boolean match(String name) {
            return true;
        }
    };

    static class ExactMatch extends EntryFilter {
        protected String pattern;

        public ExactMatch(String pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean match(String name) {
            return name.equals(pattern);
        }
    }

    static class Filter extends EntryFilter {
        protected String[] parts;

        protected boolean startAny;

        protected boolean endAny;

        public Filter(String[] parts, boolean startAny, boolean endAny) {
            this.startAny = startAny;
            this.endAny = endAny;
            this.parts = parts;
        }

        @Override
        public boolean match(String name) {
            int len = name.length();
            int i = 0;
            int p = 0;
            if (startAny) {
                i = name.indexOf(parts[p]);
                if (i == -1) {
                    return false;
                }
                i += parts[p++].length();
            } else if (!name.startsWith(parts[p])) {
                return false;
            } else {
                i += parts[p++].length();
            }
            while (i < len && p < parts.length) {
                i = name.indexOf(parts[p], i);
                if (i == -1) {
                    return false;
                }
                i += parts[p++].length();
            }
            if (p < parts.length) {
                return p == parts.length - 1 && endAny;
            } else if (i < len) {
                return endAny;
            }
            return true;
        }
    }

    public static EntryFilter newFilter(String pattern) {
        if (pattern == null || pattern.equals("*")) {
            return ANY;
        }
        ArrayList<String> parts = new ArrayList<>();
        int s = 0;
        boolean startAny = false;
        boolean endAny = false;
        if (pattern.startsWith("*")) {
            s++;
            startAny = true;
        }
        int i = pattern.indexOf('*', s);
        while (i > -1) {
            parts.add(pattern.substring(s, i));
            s = i + 1;
            i = pattern.indexOf('*', s);
        }
        if (s < pattern.length()) {
            parts.add(pattern.substring(s));
        }
        if (s == pattern.length()) {
            endAny = true;
        }
        if (parts.isEmpty()) {
            return new ExactMatch(pattern);
        }
        return new Filter(parts.toArray(new String[parts.size()]), startAny, endAny);
    }

    public abstract boolean match(String name);

}
