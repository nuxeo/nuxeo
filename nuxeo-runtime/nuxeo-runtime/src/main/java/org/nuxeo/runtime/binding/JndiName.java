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

package org.nuxeo.runtime.binding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.Name;

/**
 * Optimized JNDI name implementation. Uses UNIX path syntax.
 * <p>
 * Default Java implementation is not at all optimized.
 * <p>
 * This class is not thread safe
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JndiName implements Name {

    private static final long serialVersionUID = -4112999828077524879L;

    public static final char SEP = '/';
    protected static final String[] NO_SEGMENTS = new String[0];

    protected String[] segments;
    protected int hashCode;

    public JndiName() {
    }


    public JndiName(String ... segments) {
        this.segments = segments;
    }

    public JndiName(String path) {
        init(path);
    }

    protected void init(String path) {
        List<String> segments = new ArrayList<String>();
        int slash = 0;
        int off = 0;
        int cnt = 0;
        int len = path.length();
        if (len == 0) {
            this.segments = NO_SEGMENTS;
            return;
        }
        if (len == 1) {
            char c = path.charAt(0);
            if (c == '/') {
                this.segments = NO_SEGMENTS;
                return;
            } else if (c == '.'){
                this.segments = NO_SEGMENTS;
                return;
            } else {
                this.segments = new String[] {path};
                return;
            }
        }
        char[] chars = path.toCharArray();
        for (int i = 0; i < len; i++) {
            char c = chars[i];
            switch (c) {
            case SEP:
                if (slash == 0) { // first slash
                    if (cnt > 0) { // segment end
                        segments.add(new String(chars, off, cnt));
                        cnt = 0;
                    }
                    off = i;
                } else { // ignore double slashes
                    off++;
                }
                slash++;
                break;
            case '.':
                if (slash > 0 || i == 0) {
                    if (i < chars.length-2) { // look ahead 2 chars
                        char c1 = chars[i+1];
                        char c2 = chars[i+2];
                        if (c1 == '.' && c2 == SEP) { // a .. segment
                            if (segments.isEmpty()) { // add a dot segment
                                segments.add("..");
                            } else { // remove last segment
                                segments.remove(segments.size()-1);
                            }
                            i += 2;
                            slash = 1;
                            continue;
                        } else if (c1 == SEP) { // a . segment - ignore it
                            i++;
                            slash = 1;
                            continue;
                        }
                    } else if (i < chars.length -1 && chars[i+1] == '/') { // ignore . segment
                        slash = 0;
                        continue; //TODO - we may add here the segment to avoid rereading the char
                    }
                    slash = 0;
                }
                // do nothing (the char will be added to the segment)
            default:
                if (slash > 0) {
                    slash = 0;
                    off = i;
                }
                cnt++;
                break;
            }
        }
        if (cnt > 0) {
            segments.add(new String(chars, off, cnt));
        }
        int size = segments.size();
        if (size == 0) {
            this.segments = NO_SEGMENTS;
        } else {
            this.segments = segments.toArray(new String[segments.size()]);
        }
    }


    public Name add(int posn, String comp) throws InvalidNameException {
        if (posn < 0 && posn > segments.length) {
            throw new ArrayIndexOutOfBoundsException(posn);
        }
        String[] tmp = new String[segments.length+1];
        if (posn == 0) {
            System.arraycopy(segments, 0, tmp, 1, segments.length);
            tmp[0] = comp;
        } else if (posn == segments.length) {
            System.arraycopy(segments, 0, tmp, 0, segments.length);
            tmp[segments.length] = comp;
        } else {
            System.arraycopy(segments, 0, tmp, 0, posn);
            tmp[posn] = comp;
            System.arraycopy(segments, posn, tmp, posn+1, segments.length-posn);
        }
        segments = tmp;
        hashCode = 0;
        return this;
    }

    public Name add(String comp) throws InvalidNameException {
        String[] tmp = new String[segments.length+1];
        System.arraycopy(segments, 0, tmp, 0, segments.length);
        tmp[segments.length] = comp;
        segments = tmp;
        hashCode = 0;
        return this;
    }

    public Name addAll(Name suffix) throws InvalidNameException {
        int n = suffix.size();
        if (n > 0) {
            int len = segments.length;
            String[] tmp = new String[len+n];
            System.arraycopy(segments, 0, tmp, 0, len);
            segments = tmp;
            for (int i=0; i<n; i++) {
                segments[len+i] = suffix.get(i);
            }
        }
        hashCode = 0;
        return this;
    }

    public Name addAll(int posn, Name name) throws InvalidNameException {
        if (posn < 0 && posn > segments.length) {
            throw new ArrayIndexOutOfBoundsException(posn);
        }
        int size = name.size();
        if (size == 0) { // nothing to do
            return this;
        }
        String[] tmp = new String[segments.length+size];
        if (posn == 0) {
            for (int i=0; i<size; i++) {
                tmp[i] = name.get(i);
            }
            System.arraycopy(segments, 0, tmp, size, segments.length);
        } else if (posn == segments.length) {
            System.arraycopy(segments, 0, tmp, 0, segments.length);
            for (int i=0; i<size; i++) {
                tmp[i] = name.get(i);
            }
        } else {
            System.arraycopy(segments, 0, tmp, 0, posn);
            for (int i=0; i<size; i++) {
                tmp[i] = name.get(i);
            }
            System.arraycopy(segments, posn, tmp, posn+size, segments.length);
        }
        segments = tmp;
        hashCode = 0;
        return this;
    }


    public Object remove(int posn) throws InvalidNameException {
        if (posn <0 || posn >= segments.length) {
            throw new ArrayIndexOutOfBoundsException(posn);
        }
        String comp = segments[posn];
        String[] tmp = new String[segments.length-1];
        if (posn == 0) {
            System.arraycopy(segments, 1, tmp, 0, tmp.length);
        } else if (posn == segments.length -1) {
            System.arraycopy(segments, 0, tmp, 0, tmp.length);
        } else {
            System.arraycopy(segments, 0, tmp, 0, posn);
            System.arraycopy(segments, posn+1, tmp, posn, segments.length-posn-1);
        }
        segments = tmp;
        hashCode = 0;
        return comp;
    }

    public boolean endsWith(Name n) {
        int size = n.size();
        if (size > segments.length) {
            return false;
        }
        if (size  == 0) {
            return true;
        }
        int s = segments.length - size;
        for (int i = segments.length-1, k=size-1; i>=s; i--, k--) {
            if (!segments[i].equals(n.get(k))) {
                return false;
            }
        }
        return true;
    }

    public boolean startsWith(Name n) {
        int size = n.size();
        if (size > segments.length) {
            return false;
        }
        if (size  == 0) {
            return true;
        }
        for (int i=0; i<size; i++) {
            if (!segments[i].equals(n.get(i))) {
                return false;
            }
        }
        return true;
    }

    public String get(int posn) {
        return segments[posn];
    }

    public boolean isEmpty() {
        return segments.length == 0;
    }

    public int size() {
        return segments.length;
    }

    public Name getPrefix(int posn) {
        if (posn <0 || posn > segments.length) {
            throw new ArrayIndexOutOfBoundsException(posn);
        }
        String[] tmp = new String[posn];
        if (posn > 0) {
            System.arraycopy(segments, 0, tmp, 0, tmp.length);
        }
        return new JndiName(tmp);
    }

    public Name getSuffix(int posn) {
        if (posn <0 || posn > segments.length) {
            throw new ArrayIndexOutOfBoundsException(posn);
        }
        String[] tmp = new String[segments.length - posn];
        if (posn < segments.length) {
            System.arraycopy(segments, posn, tmp, 0, tmp.length);
        }
        return new JndiName(tmp);
    }

    public Enumeration<String> getAll() {
        return Collections.enumeration(Arrays.asList(segments));
    }

    @Override
    public Object clone() {
        // we don't need to clone segments since their element are immutable
        return new JndiName(segments);
    }

    public int compareTo(Object obj) {
        Name name = (Name)obj;
        int size = name.size();
        int r = segments.length - size;
        if (r != 0) {
            return r;
        }
        // the same number of segments
        for (int i=0; i<size; i++) {
            r = segments[i].compareTo(name.get(i));
            if (r != 0) {
                return r;
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        if (segments.length == 0) {
            return "";
        }
        if (segments.length == 1) {
            return segments[0];
        }
        StringBuilder buf = new StringBuilder(segments.length*16);
        buf.append(segments[0]);
        for (int i=1; i<segments.length; i++) {
            buf.append("/").append(segments[i]);
        }
        return buf.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj instanceof Name) {
            Name n = (Name)obj;
            if (segments.length == n.size()) {
                for (int i=0; i<segments.length; i++) {
                    if (!segments[i].equals(n.get(i))) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = computeHashCode();
        }
        return hashCode;
    }


    protected int computeHashCode() {
        int hash = 17;
        int segmentCount = segments.length;
        for (int i = 0; i < segmentCount; i++) {
            //this function tends to given a fairly even distribution
            hash = hash * 37 + segments[i].hashCode();
        }
        return hash;
    }

}
