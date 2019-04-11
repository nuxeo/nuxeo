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
package org.nuxeo.ecm.webengine.jaxrs.servlet.mapping;

import java.util.Arrays;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public final class Path {

    public static final int HAS_LEADING_SLASH = 1;

    public static final int HAS_TRAILING_SLASH = 2;

    public static final String[] EMPTY_SEGMENTS = new String[0];

    public static final Path ROOT = new Path(EMPTY_SEGMENTS, HAS_LEADING_SLASH | HAS_TRAILING_SLASH);

    public static final Path EMPTY = new Path(EMPTY_SEGMENTS);

    public static Path parse(String path) {
        return new PathParser().parse(path);
    }

    protected int bits;

    protected final String[] segments;

    public Path(String[] segments) {
        this(segments, 0);
    }

    public Path(String[] segments, int bits) {
        this(segments, bits, true);
    }

    protected Path(String[] segments, int bits, boolean updateHashCode) {
        this.segments = segments;
        this.bits = bits;
        if (updateHashCode) {
            updateHashCode();
        }
    }

    public int length() {
        return segments.length;
    }

    public String[] segments() {
        return segments;
    }

    public boolean hasLeadingSlash() {
        return (bits & HAS_LEADING_SLASH) == HAS_LEADING_SLASH;
    }

    public boolean hasTrailingSlash() {
        return (bits & HAS_TRAILING_SLASH) == HAS_TRAILING_SLASH;
    }

    public boolean isAbsolute() {
        return (bits & HAS_LEADING_SLASH) == HAS_LEADING_SLASH;
    }

    public Path copy() {
        return new Path(segments, bits, false);
    }

    public Path copy(int bits) {
        return new Path(segments, (bits & ~3) | (bits & 3), false);
    }

    @Override
    public String toString() {
        int len = segments.length;
        if (len == 0) {
            return hasLeadingSlash() || hasTrailingSlash() ? "/" : "";
        }
        StringBuilder buf = new StringBuilder(segments.length * 16);
        if (hasLeadingSlash()) {
            buf.append('/');
        }
        buf.append(segments[0]);
        for (int i = 1; i < segments.length; i++) {
            buf.append('/').append(segments[i]);
        }
        if (hasTrailingSlash()) {
            buf.append('/');
        }
        return buf.toString();
    }

    public String lastSegment() {
        return segments.length == 0 ? "" : segments[segments.length - 1];
    }

    public String getFileExtension() {
        if (segments.length == 0) {
            return null;
        }
        String last = segments[segments.length - 1];
        int i = last.lastIndexOf('.');
        return i > -1 ? last.substring(i + 1) : null;
    }

    public String getFileName() {
        if (segments.length == 0) {
            return "";
        }
        String last = segments[segments.length - 1];
        int i = last.lastIndexOf('.');
        return i > -1 ? last.substring(0, i) : null;
    }

    public Path append(String segment) {
        String[] ar = new String[segments.length];
        System.arraycopy(segments, 0, ar, 0, segments.length);
        return new Path(segments, bits);
    }

    public Path makeAbsolute() {
        return hasLeadingSlash() ? this : new Path(segments, bits | HAS_LEADING_SLASH);
    }

    public Path makeRelative() {
        return hasLeadingSlash() ? new Path(segments, bits & ~HAS_LEADING_SLASH) : this;
    }

    public Path removeTrailingSlash() {
        return hasTrailingSlash() ? new Path(segments, bits & ~HAS_TRAILING_SLASH) : this;
    }

    public boolean isRoot() {
        return segments.length == 0 && hasLeadingSlash();
    }

    public String segment(int i) {
        return segments[i];
    }

    public Path removeLastSegment() {
        return removeLastSegments(1);
    }

    public Path removeLastSegments(int i) {
        String[] ar = new String[segments.length];
        System.arraycopy(segments, 0, ar, 0, segments.length);
        return new Path(segments, bits);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj.getClass() == Path.class) {
            Path path = (Path) obj;
            return path.bits == bits && Arrays.equals(path.segments, segments);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return bits;
    }

    private void updateHashCode() {
        bits = (bits & 3) | (computeHashCode() << 2);
    }

    private int computeHashCode() {
        int hash = 17;
        int segmentCount = segments.length;
        for (int i = 0; i < segmentCount; i++) {
            // this function tends to given a fairly even distribution
            hash = hash * 37 + segments[i].hashCode();
        }
        return hash;
    }

}
