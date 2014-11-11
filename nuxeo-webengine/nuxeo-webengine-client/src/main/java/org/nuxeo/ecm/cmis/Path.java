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

package org.nuxeo.ecm.cmis;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Path implements Serializable {

    public static final char SEP = '/';

    protected static final int HAS_LEADING = 1;
    protected static final int HAS_TRAILING = 2;
    protected static final int HASH_MASK = ~HAS_TRAILING;
    protected static final int ALL_SEPARATORS = HAS_LEADING | HAS_TRAILING;
    protected static final int USED_BITS = 2;
    protected static final String[] NO_SEGMENTS = new String[0];

    private static final long serialVersionUID = 5008948361159403627L;

    public static final Path EMPTY = new Path(new String[0], 0) {
        private static final long serialVersionUID = -1731993368728652448L;
        @Override
        public String toString() {
            return "";
        }
    };

    public static final Path ROOT = new Path(new String[0], 1) {
        private static final long serialVersionUID = -6689687769363666578L;
        @Override
        public String toString() {
            return "/";
        }
    };

    protected String[] segments;
    protected int flags;


    public Path(String path) {
        init(path);
        updateHashCode();
    }

    public Path(String path, int flags) {
        init(path);
        this.flags |= flags & ALL_SEPARATORS;
        updateHashCode();
    }

    public Path(String[] segments, int flags) {
        this.segments = segments;
        this.flags = flags;
        updateHashCode();
    }

    public Path(Path path) {
        segments = path.segments;
        flags = path.flags;
    }


    private void init(String path) {
        List<String> segments = new ArrayList<String>();
        int slash = 0;
        int off = 0;
        int cnt = 0;
        int len = path.length();
        if (len == 0) {
            flags = 0;
            this.segments = NO_SEGMENTS;
            return;
        }
        if (len == 1) {
            char c = path.charAt(0);
            if (c == '/') {
                flags = HAS_LEADING;
                this.segments = NO_SEGMENTS;
                return;
            } else if (c == '.'){
                flags = 0;
                this.segments = NO_SEGMENTS;
                return;
            } else {
                flags = 0;
                this.segments = new String[] {path};
                return;
            }
        }
        char[] chars = path.toCharArray();
        flags = chars[0] == '/' ? HAS_LEADING : 0;
        if (chars[len-1] == '/') {
            flags |= HAS_TRAILING;
        }
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
                                int last = segments.size()-1;
                                if (!"..".equals(segments.get(last))) {
                                    segments.remove(last);
                                } else {
                                    segments.add("..");
                                }
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

    protected final void updateHashCode() {
        flags = (flags & ALL_SEPARATORS) | (computeHashCode() << USED_BITS);
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

    public boolean isAbsolute() {
        return (flags & HAS_LEADING) != 0;
    }

    public boolean isRelative() {
        return (flags & HAS_LEADING) == 0;
    }

    public boolean isRoot() {
        return (segments.length == 0) && ((flags & HAS_LEADING) != 0);
    }

    public boolean isEmpty() {
        return (segments.length == 0) && ((flags & HAS_LEADING) == 0);
    }

    public boolean hasTrailingSeparator() {
        return (flags & HAS_TRAILING) != 0;
    }

    public int segmentCount() {
        return segments.length;
    }

    public String[] segments() {
        final String[] segmentCopy = new String[segments.length];
        System.arraycopy(segments, 0, segmentCopy, 0, segments.length);
        return segmentCopy;
    }

    public String segment(int index) {
        return segments[index];
    }

    public String lastSegment() {
        int len = segments.length;
        return len == 0 ? null : segments[len - 1];
    }

    public String getFileExtension() {
        int len = segments.length;
        if (len == 0) {
            return null;
        }
        String name = segments[len - 1];
        int p = name.lastIndexOf('.');
        if (p > -1) {
            return name.substring(p+1);
        }
        return null;
    }

    public String getFileName() {
        int len = segments.length;
        if (len == 0) {
            return null;
        }
        String name = segments[len - 1];
        int p = name.lastIndexOf('.');
        if (p > -1) {
            return name.substring(0, p);
        }
        return name;
    }

    public String[] getFileParts() {
        int len = segments.length;
        if (len == 0) {
            return null;
        }
        String name = segments[len - 1];
        int p = name.lastIndexOf('.');
        if (p > -1) {
            return new String[] {name.substring(0, p), name.substring(p+1)};
        }
        return new String[] {name, null};
    }

    public Path makeAbsolute() {
        if (isAbsolute()) {
            return this;
        }
        int k = 0;
        for (int i=0; i<segments.length; i++) {
            if ("..".equals(segments[i])) {
                k++;
            } else {
                break;
            }
        }
        if (k > 0) {
            String[] newSegments = new String[segments.length-k];
            System.arraycopy(segments, k, newSegments, 0, newSegments.length);
           return  new Path(newSegments, flags | HAS_LEADING);
        } 
        return new Path(segments, flags | HAS_LEADING);
    }

    public Path makeRelative() {
        if (isRelative()) {
            return this;
        }
        return new Path(segments, flags & ~HAS_LEADING);
    }

    public Path removeTrailingSeparator() {
        if (!hasTrailingSeparator()) {
            return this;
        }
        return new Path(segments, flags & ~HAS_TRAILING);
    }

    public Path addTrailingSeparator() {
        if (hasTrailingSeparator()) {
            return this;
        }
        return new Path(segments, flags | HAS_TRAILING);
    }

    public Path removeLastSegments(int count) {
        if (count == 0) {
            return this;
        }
        if (count >= segments.length) {
            //result will have no trailing separator
            return (flags & HAS_LEADING) != 0 ? ROOT : EMPTY;
        }
        assert count > 0;
        final int newSize = segments.length - count;
        final String[] newSegments = new String[newSize];
        System.arraycopy(segments, 0, newSegments, 0, newSize);
        return new Path(newSegments, flags);
    }

    public Path removeFirstSegments(int count) {
        if (count == 0) {
            return this;
        }
        if (count >= segments.length) {
            return EMPTY;
        }
        assert count > 0;
        int newSize = segments.length - count;
        String[] newSegments = new String[newSize];
        System.arraycopy(segments, count, newSegments, 0, newSize);

        //result is always a relative path
        return new Path(newSegments, flags & HAS_TRAILING);
    }

    public Path removeFileExtension() {
        String extension = getFileExtension();
        if (extension == null || extension.equals("")) { //$NON-NLS-1$
            return this;
        }
        String lastSegment = lastSegment();
        int index = lastSegment.lastIndexOf(extension) - 1;
        return removeLastSegments(1).append(lastSegment.substring(0, index));
    }

    public Path up() {
        return removeLastSegments(1);
    }

    public Path getParent() {
        return removeLastSegments(1);
    }

    public Path appendSegment(String segment) {
        int myLen = segments.length;
        String[] newSegments = new String[myLen + 1];
        System.arraycopy(segments, 0, newSegments, 0, myLen);
        newSegments[myLen] = segment;
        return new Path(newSegments, flags);
    }

    public Path append(Path tail) {
        //optimize some easy cases
        if (tail == null || tail.segmentCount() == 0) {
            return this;
        }
        if (isEmpty()) {
            return tail.makeRelative();
        }
        if (isRoot()) {
            return tail.makeAbsolute();
        }

        int flags = this.flags;
        
        int myLen = segments.length;
        int tailLen = tail.segmentCount();
        int j = 0; int s = 0;
        // remove ../ segments from the appended path
        for (int i=0; i<tailLen; i++) {
            String seg = tail.segments[i];
            if ("..".equals(seg)) {
                j++;
            } else if (".".equals(seg)) {
                if (j == 0) s++;
            } else {
                break;
            }
        }
        if (j > 0) s = j;
        
        int k = myLen - j;
        if (k < 0) {
            myLen = -k;
        } else {
            myLen = k;
        }
        
        //concatenate the two segment arrays
        String[] newSegments = new String[myLen + tailLen-s];
        if (k < 0) { 
            for (int i = 0; i < myLen; i++) {
                newSegments[i] = "..";    
            }            
            flags &= ~HAS_LEADING;
        } else if (k > 0) {
            System.arraycopy(segments, 0, newSegments, 0, myLen);
        }
        for (int i = s; i < tailLen; i++) {
            newSegments[myLen + i-s] = tail.segment(i);
        }
        //use my leading separators and the tail's trailing separator
        
        return new Path(newSegments, (flags & HAS_LEADING) | (tail.hasTrailingSeparator() ? HAS_TRAILING : 0));
    }

    public Path append(String tail) {
        //optimize addition of a single segment
        if (tail.indexOf(SEP) == -1) {
            int tailLength = tail.length();
            if (tailLength < 3) {
                //some special cases
                if (tailLength == 0 || ".".equals(tail)) {
                    return this;
                }
                if ("..".equals(tail)) {
                    return removeLastSegments(1);
                }
            }
            //just add the segment
            int myLen = segments.length;
            String[] newSegments = new String[myLen + 1];
            System.arraycopy(segments, 0, newSegments, 0, myLen);
            newSegments[myLen] = tail;
            return new Path(newSegments, flags & ~HAS_TRAILING);
        }
        //go with easy implementation
        return append(new Path(tail));
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Path)) {
            return false;
        }
        Path target = (Path) obj;
        //check leading separators and hash code
        if (flags != target.flags) {
            return false;
        }
        String[] targetSegments = target.segments;
        int i = segments.length;
        //check segment count
        if (i != targetSegments.length) {
            return false;
        }
        //check segments in reverse order - later segments more likely to differ
        while (--i >= 0) {
            if (!segments[i].equals(targetSegments[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return flags & HASH_MASK;
    }

    @Override
    public String toString() {
        int len = segments.length;
        if (len == 0) {
            return (flags & HAS_LEADING) != 0 ? "/" : "";
        }
        StringBuilder buf = new StringBuilder(len*16);
        if ((flags & HAS_LEADING) != 0) {
            buf.append(SEP);
        }
        buf.append(segments[0]);
        for (int i=1; i<len; i++) {
            buf.append(SEP).append(segments[i]);
        }
        if ((flags & HAS_TRAILING) != 0) {
            buf.append(SEP);
        }
        return buf.toString();
    }

    public static void main(String[] args) {
//
//        System.out.println(new Path("abc/asdf/file.ext"));
//        System.out.println(new org.nuxeo.common.utils.Path("abc/asdf/file.ext"));
//
//        System.out.println(new Path("/abc/asdf/file.ext"));
//        System.out.println(new org.nuxeo.common.utils.Path("/abc/asdf/file.ext"));
//
//        System.out.println(new Path("/./abc//asdf/../file.ext"));
//        System.out.println(new org.nuxeo.common.utils.Path("/./abc//asdf/../file.ext"));
//
//        System.out.println("----------------------");
//
//        double s = System.currentTimeMillis();
//        for (int i=0; i<100000; i++) {
//            new Path("/./abc//asdf/../file.ext");
//        }
//        System.out.println("new path: >>>> "+(System.currentTimeMillis()-s)/1000);
//
//        s = System.currentTimeMillis();
//        for (int i=0; i<100000; i++) {
//            new org.nuxeo.common.utils.Path("/./abc//asdf/../file.ext");
//        }
//        System.out.println("old path: >>>> "+(System.currentTimeMillis()-s)/1000);
        
        Path p = new Path("/commands");
        System.out.println(p);
        System.out.println(p=p.append("test"));
        System.out.println(p=p.append("../../../test2"));
        System.out.println(p=p.makeAbsolute());
        //System.out.println(p=p.append(".."));
        //System.out.println(p=p.append(".."));
    }

}
