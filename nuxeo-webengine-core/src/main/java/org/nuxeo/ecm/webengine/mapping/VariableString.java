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
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class VariableString {

    public static final Pattern DOLAR_PATTERN = Pattern.compile("\\$([0-9]+|[A-Za-z_][A-Za-z_0-9]*)");


    protected ReplacementSegment[] segments;
    protected String value; // constant value if any

    public VariableString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Argument cannot be null");
        }
        this.value = value;
    }

    public boolean isConstant() {
        return value != null;
    }

    public VariableString(ReplacementSegment[] segments) {
        if (segments == null) {
            throw new IllegalArgumentException("Argument cannot be null");
        }
        this.segments = segments;
    }

    public final String getValue(Attributes attrs) {
        if (value != null) { // constant value
            return value;
        }
        if (segments.length == 1) {
            return segments[0].getReplacement(attrs);
        }
        StringBuilder buf = new StringBuilder(segments.length * 16);
        for (ReplacementSegment segment : segments) {
            if (segment != null) { // avoid to add null segments (ignore undefined vars)
                buf.append(segment.getReplacement(attrs));
            }
        }
        return buf.toString();
    }


    public static VariableString parse(String replacement) {
        Matcher m = DOLAR_PATTERN.matcher(replacement);
        if (!m.find()) {
            return new VariableString(replacement);
        }

        List<ReplacementSegment> ar = new ArrayList<ReplacementSegment>();
        int s = 0;
        do {
            int e = m.start();
            // add segment
            if (e > s) {
                ar.add(new StringSegment(replacement.substring(s, e)));
            }
            // add dolar
            String var = m.group(1);
            if (Character.isDigit(var.charAt(0))) {
                ar.add(new IndexedSegment(Integer.parseInt(var)));
            } else {
                ar.add(new NamedSegment(var));
            }
            s = m.end();
        } while (m.find());
        if (s < replacement.length()) {
            // add segment
            ar.add(new StringSegment(replacement.substring(s)));
        }
        ReplacementSegment[] segments = ar.toArray(new ReplacementSegment[ar.size()]);
        return new VariableString(segments);
    }

}
