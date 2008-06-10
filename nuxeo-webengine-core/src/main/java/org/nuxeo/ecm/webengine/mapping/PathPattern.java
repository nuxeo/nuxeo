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

package org.nuxeo.ecm.webengine.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.ecm.webengine.util.Attributes;


/**
 * Match patterns of the type:
 *
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PathPattern  {

    static final Pattern NAMED_WILDCARD = Pattern.compile("[A-Za-z_][A-Za-z_0-9]*");

    final Pattern pattern;
    String[] vars;

    public PathPattern(String pattern) {
        pattern = processNamedWildcards(pattern);
        this.pattern = Pattern.compile(pattern);
    }

    public String tr(String pattern) {
        return processNamedWildcards(pattern);
    }

    public String processNamedWildcards(String pattern) {
        StringBuilder buf = new StringBuilder();
        List<String> names = new ArrayList<String>();
        names.add("path");
        int len = pattern.length();
        boolean esc = false;
        int inKlass = 0, s = 0;
        for (int i=0; i<len; i++) {
            char c = pattern.charAt(i);
            if (c == '\\') {
                if (!esc) { esc = true; continue; } else { esc = true; }
            }
            switch (c) {
            case '[':
                inKlass++;
                break;
            case ']':
                inKlass--;
                break;
            case '(':
                if (i + 1 == len) {
                    continue; // parse error
                }
                // i + 1 > len
                char c1 = pattern.charAt(i+1);
                if (c1 != '?') { names.add(null); continue; }
                if (i + 2 == len) {
                    continue;  //parse error
                }
                char c2 = pattern.charAt(i+2);
                if (c2 == ':' || c2 == '>') {
                    continue; // not a group
                }
                // should be a named group
                int p = pattern.indexOf(':', i+2);
                if (p == -1) { continue; } // parse error
                String name = pattern.substring(i+2, p);
                // check name against the regex
                Matcher m = NAMED_WILDCARD.matcher(name);
                if (m.matches()) {
                    names.add(name);
                    if (i > s) {
                        buf.append(pattern.substring(s, i));
                    }
                    buf.append("(");
                    s = p+1;
                } else {
                    continue; //parse error
                }
                i = p;
            }
        }
        vars = names.toArray(new String[names.size()]);
        if (s > 0) {
            if (s < len) {
                buf.append(pattern.substring(s)); //append the end
            }
            return buf.toString();
        } else { // no named wildcards
            return pattern;
        }
    }

    public Attributes match(String input) {
        Matcher m = pattern.matcher(input);
        if (m.matches()) {
            return createMapping(m);
        }
        return null;
    }

    protected final Attributes createMapping(Matcher m) {
        Attributes attrs = new Attributes(vars.length);
        int n = m.groupCount();
        for (int i=0; i<=n; i++) {
            attrs.add(vars[i], m.group(i));
        }
        return attrs;
    }

}
