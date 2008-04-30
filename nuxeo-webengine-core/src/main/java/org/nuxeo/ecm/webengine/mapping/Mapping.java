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

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Mapping {

    MappingDescriptor mdef;

    String siteName;
    String[] vars;
    int size = 0;
    String[] traversalPath;
    String script;

    public Mapping() {
        this(null, 4);
    }

    public Mapping(String siteName, int size) {
        this.siteName = siteName;
        vars = new String[size<<1];
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setType(String type) {
        addVar("type", type);
    }

    public String getValue(int off) {
        return vars[(off<<1)+1];
    }

    public String getName(int off) {
        return vars[off<<1];
    }

    public int size() {
        return size>>1;
    }

    public int indexOf(String name) {
        for (int i = 0; i < size; i += 2) {
            if (name.equals(vars[i])) {
                return i >> 1;
            }
        }
        return -1;
    }

    public String getValue(String name) {
        for (int i = 0; i < size; i += 2) {
            if (name.equals(vars[i])) {
                return vars[i + 1];
            }
        }
        return null;
    }

    public void addVar(String name, String value) {
        if (size == vars.length) { // resize
            String[] newVars = new String[size+8];
            System.arraycopy(vars, 0, newVars, 0, size);
            vars = newVars;
        }
        vars[size++] = name;
        vars[size++] = value;
    }

    public void setValue(String name, String value) {
        for (int i = 0; i < size; i += 2) {
            if (name.equals(vars[i])) {
                vars[i + 1] = value;
            }
        }
    }

    public void setValue(int off, String value) {
        vars[(off<<1)+1] = value;
    }

    public void setName(int off, String name) {
        vars[off<<1] = name;
    }

    public final String[] resolveSegments(ReplacementSegment[] segments) {
        String[] result = new String[segments.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = segments[i].getReplacement(this);
        }
        return result;
    }

    public final String toString(ReplacementSegment[] segments) {
        if (segments.length == 1) {
            return segments[0].getReplacement(this);
        }
        StringBuilder buf = new StringBuilder(segments.length * 16);
        for (ReplacementSegment segment : segments) {
            buf.append(segment.getReplacement(this));
        }
        return buf.toString();
    }

    public String getScript() {
        if (script == null) {
            if (mdef.script != null) {
                script = toString(mdef.script);
            }
        }
        return script;
    }

    public String[] getTraversalPath() {
        return traversalPath;
    }

    public MappingDescriptor getDescriptor() {
        return mdef;
    }

    public boolean isDynamic() {
        return mdef == null;
    }

}
