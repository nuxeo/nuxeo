/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.osgi.util;

import java.util.ArrayList;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class EntryFilter {

    public static final EntryFilter ANY = new EntryFilter() {
        public boolean match(String name) { return true; }
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
                return p == parts.length-1 && endAny;
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
        ArrayList<String> parts = new ArrayList<String>();
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
            s = i+1;
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
