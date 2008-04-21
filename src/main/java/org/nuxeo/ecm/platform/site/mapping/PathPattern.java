/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.site.mapping;

import java.util.regex.Matcher;




/**
 * Match patterns of the type:
 *
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PathPattern  {

    java.util.regex.Pattern pattern;
    String[] vars;

    public PathPattern(String pattern) {
        this (pattern, null);
    }

    public PathPattern(String pattern, String[] vars) {
        pattern = tr(pattern);
        System.out.println("> "+pattern);
        this.pattern = java.util.regex.Pattern.compile(pattern);
        if (vars == null) { // init vars
            Matcher m = this.pattern.matcher("");
            this.vars = new String[m.groupCount()+1];
            this.vars[0] = "url";
        } else {
            this.vars = vars;
        }
    }

    public String tr(String pattern) {
        if (pattern.startsWith("**/")) {
            pattern = "(?:(.*)/)?"+pattern.substring(2);
        }
        if (pattern.endsWith("/**")) {
            pattern = pattern.substring(0, pattern.length()-2)+"(.*)";
        }
        int p = pattern.indexOf("/**/");
        while (p > -1) {
            pattern = pattern.substring(0, p)+"(?:/(.*))?/"+pattern.substring(p+4);
            p = pattern.indexOf("/**/", p+1);
        }
        if (pattern.startsWith("*/")) {
            pattern = "([^/]+)"+pattern.substring(1);
        }
        if (pattern.endsWith("/*")) {
            pattern = pattern.substring(0, pattern.length()-1)+"([^/]+)";
        }
        p = pattern.indexOf("/*/");
        while (p > -1) {
            pattern = pattern.substring(0, p+1)+"([^/]+)"+pattern.substring(p+2);
            p = pattern.indexOf("/*/", p+1);
        }
//        String[] ar = pattern.split("[^\\]?\\(");
        return pattern;
    }

//    java.util.regex.Pattern NAMED_WILDCARD = java.util.regex.Pattern.compile("\\(\\?[A-Za-z]+:");
//    public void processNamedWildcards(String pattern) {
//        StringBuilder buf = new StringBuilder(pattern.length());
//        int k = 0;
//        int len = pattern.length();
//        int esc = 0;
//        int inklass = 0;
//        TOP: for (int i=0; i<len; i++) {
//            char c = pattern.charAt(i);
//            switch (c) {
//            case '\\':
//                esc = (esc + 1) % 2;
//                break;
//            case '[':
//            case ']':
//            case '(':
//                if (esc > 0) continue;
//                if (inklass > 0) continue;
//                if (i+3 >= len) break TOP;
//                k++;
//                if (pattern.charAt(i+1) == '?') {
//                    int start = i+2;
//                    for (int j=start; j<len; j++) {
//                        if (pattern.charAt(j) == ':') {
//                            String name = pattern.substring(start, j);
//                            buf.append(b)
//                            break;
//                        }
//                    }
//                    i = start;
//                }
//            default:
//
//            }
//        }
//        while (i > -1) {
//            if ((i > 1) && pattern.charAt(i-1) != '\\') {
//                NAMED_WILDCARD
//                if ((i < len-3) && pattern.indexOf("?:", i+1)) {
//
//                }
//            }
//            i = pattern.indexOf('(');
//        }
//    }


    public Mapping match(String input) {
        Matcher m = pattern.matcher(input);
        if (m.matches()) {
            Mapping mapping = new Mapping();
            int n = m.groupCount();
            for (int i=0; i<=n; i++) {
                mapping.addVar(vars[i], m.group(i));
            }
            return mapping;
        }
        return null;
    }

}
