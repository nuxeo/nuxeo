/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.common.utils;

import java.io.Serializable;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Path implements Serializable {

    private static final long serialVersionUID = -5420562131803786641L;
    private static final int HAS_LEADING = 1;
    private static final int HAS_TRAILING = 2;
    private static final int ALL_SEPARATORS = HAS_LEADING | HAS_TRAILING;
    private static final int USED_BITS = 2;

    private static final String[] NO_SEGMENTS = new String[0];

    /**
     * Path separator character constant "/" used in paths.
     */
    private static final char SEPARATOR = '/';

    /** Constant empty string value. */
    private static final String EMPTY_STRING = ""; //$NON-NLS-1$

    /** Constant value containing the empty path with no device. */
    private static final Path EMPTY = new Path(EMPTY_STRING);
    //
    private static final int HASH_MASK = ~HAS_TRAILING;

    /** Constant root path string (<code>"/"</code>). */
    private static final String ROOT_STRING = "/"; //$NON-NLS-1$

    /** Constant value containing the root path with no device. */
    private static final Path ROOT = new Path(ROOT_STRING);

    private String[] segments;
    private int separators;

    /**
     * Constructs a new path from the given string path.
     * <p>
     * The string path must represent a valid file system path
     * on the local file system.
     * <p>
     * The path is canonicalized and double slashes are removed
     * except at the beginning. (to handle UNC paths). All forward
     * slashes ('/') are treated as segment delimiters, and any
     * segment and device delimiters for the local file system are
     * also respected (such as colon (':') and backslash ('\') on some file systems).
     *
     * @param fullPath the string path
     * @see #isValidPath(String)
     */
    public Path(String fullPath) {
        initialize(fullPath);
    }

    /**
     * Optimized constructor - no validations on segments are done.
     *
     * @param segments
     * @param separators
     */
    private Path(String[] segments, int separators) {
        // no segment validations are done for performance reasons
        this.segments = segments;
        //hash code is cached in all but the bottom three bits of the separators field
        this.separators = (computeHashCode() << USED_BITS) | (separators & ALL_SEPARATORS);
    }

    /**
     * Creates a path object from an absolute and canonical path.
     * <p>
     * This method does not check the given path - it assumes the path
     * has a valid format of the form "/a/b/c" without duplicate slashes or dots.
     *
     * @return the path
     */
    public static Path createFromAbsolutePath(String path) {
        assert path != null;
        final int len = computeSegmentCount(path);
        if (path.length() < 2) {
            return ROOT;
        }
        String[] segments = new String[len];
        int k = 0;
        int j = 1;
        int i = path.indexOf(SEPARATOR, j);
        while (i > 0) {
            segments[k++] = path.substring(j, i);
            j = i + 1;
            i = path.indexOf(SEPARATOR, j);
        }
        segments[k] = path.substring(j);
        return new Path(segments, HAS_LEADING);
    }

    public static Path createFromSegments(String[] segments) {
        if (segments.length == 0) {
            return ROOT;
        }
        return new Path(segments, HAS_LEADING);
    }


    public Path addFileExtension(String extension) {
        if (isRoot() || isEmpty() || hasTrailingSeparator()) {
            return this;
        }
        int len = segments.length;
        String[] newSegments = new String[len];
        System.arraycopy(segments, 0, newSegments, 0, len - 1);
        newSegments[len - 1] = segments[len - 1] + '.' + extension;
        return new Path(newSegments, separators);
    }


    public Path addTrailingSeparator() {
        if (hasTrailingSeparator() || isRoot()) {
            return this;
        }
        if (isEmpty()) {
            return new Path(segments, HAS_LEADING);
        }
        return new Path(segments, separators | HAS_TRAILING);
    }

    // XXX: confusing, one may think that this modifies the path
    // being appended to (like Python's list.append()).
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

        //concatenate the two segment arrays
        int myLen = segments.length;
        int tailLen = tail.segmentCount();
        String[] newSegments = new String[myLen + tailLen];
        System.arraycopy(segments, 0, newSegments, 0, myLen);
        for (int i = 0; i < tailLen; i++) {
            newSegments[myLen + i] = tail.segment(i);
        }
        //use my leading separators and the tail's trailing separator
        Path result = new Path(newSegments, (separators & HAS_LEADING) | (tail.hasTrailingSeparator() ? HAS_TRAILING : 0));
        String tailFirstSegment = newSegments[myLen];
        if (tailFirstSegment.equals("..") || tailFirstSegment.equals(".")) { //$NON-NLS-1$ //$NON-NLS-2$
            result.canonicalize();
        }
        return result;
    }

    // XXX: same remark
    public Path append(String tail) {
        //optimize addition of a single segment
        if (tail.indexOf(SEPARATOR) == -1) {
            int tailLength = tail.length();
            if (tailLength < 3) {
                //some special cases
                if (tailLength == 0 || ".".equals(tail)) { //$NON-NLS-1$
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
            return new Path(newSegments, separators & ~HAS_TRAILING);
        }
        //go with easy implementation
        return append(new Path(tail));
    }


    /**
     * Destructively converts this path to its canonical form.
     * <p>
     * In its canonical form, a path does not have any
     * "." segments, and parent references ("..") are collapsed
     * where possible.
     * </p>
     * @return true if the path was modified, and false otherwise.
     */
    private boolean canonicalize() {
        //look for segments that need canonicalizing
        for (int i = 0, max = segments.length; i < max; i++) {
            String segment = segments[i];
            if (segment.charAt(0) == '.' && (segment.equals("..") || segment.equals("."))) { //$NON-NLS-1$ //$NON-NLS-2$
                //path needs to be canonicalized
                collapseParentReferences();
                //paths of length 0 have no trailing separator
                if (segments.length == 0) {
                    separators &= HAS_LEADING;
                }
                //recompute hash because canonicalize affects hash
                separators = (separators & ALL_SEPARATORS) | (computeHashCode() << USED_BITS);
                return true;
            }
        }
        return false;
    }

    /**
     * Destructively removes all occurrences of ".." segments from this path.
     */
    private void collapseParentReferences() {
        int segmentCount = segments.length;
        String[] stack = new String[segmentCount];
        int stackPointer = 0;
        for (int i = 0; i < segmentCount; i++) {
            String segment = segments[i];
            if (segment.equals("..")) { //$NON-NLS-1$
                if (stackPointer == 0) {
                    // if the stack is empty we are going out of our scope
                    // so we need to accumulate segments.  But only if the original
                    // path is relative.  If it is absolute then we can't go any higher than
                    // root so simply toss the .. references.
                    if (!isAbsolute()) {
                        stack[stackPointer++] = segment; //stack push
                    }
                } else {
                    // if the top is '..' then we are accumulating segments so don't pop
                    if ("..".equals(stack[stackPointer - 1])) { //$NON-NLS-1$
                        stack[stackPointer++] = ".."; //$NON-NLS-1$
                    } else {
                        stackPointer--;
                        //stack pop
                    }
                }
                //collapse current references
            } else if (!segment.equals(".") || (i == 0 && !isAbsolute())) {
                stack[stackPointer++] = segment; //stack push
            }
        }
        //if the number of segments hasn't changed, then no modification needed
        if (stackPointer == segmentCount) {
            return;
        }
        //build the new segment array backwards by popping the stack
        String[] newSegments = new String[stackPointer];
        System.arraycopy(stack, 0, newSegments, 0, stackPointer);
        segments = newSegments;
    }

    /**
     * Removes duplicate slashes from the given path.
     */
    private static String collapseSlashes(String path) {
        int length = path.length();
        // if the path is only 0 or 1 chars long then it could not possibly have illegal
        // duplicate slashes.
        if (length < 2) {
            return path;
        }
        // check for an occurrence of // in the path.  Start at index 1 to ensure we skip leading UNC //
        // If there are no // then there is nothing to collapse so just return.
        if (path.indexOf("//", 1) == -1) {
            return path;
        }
        // We found an occurrence of // in the path so do the slow collapse.
        char[] result = new char[path.length()];
        int count = 0;
        boolean hasPrevious = false;
        char[] characters = path.toCharArray();
        for (char c : characters) {
            if (c == SEPARATOR) {
                if (!hasPrevious) {
                    hasPrevious = true;
                    result[count] = c;
                    count++;
                } // else skip double slashes
            } else {
                hasPrevious = false;
                result[count] = c;
                count++;
            }
        }
        return new String(result, 0, count);
    }

    private int computeHashCode() {
        int hash = 17;
        int segmentCount = segments.length;
        for (int i = 0; i < segmentCount; i++) {
            //this function tends to given a fairly even distribution
            hash = hash * 37 + segments[i].hashCode();
        }
        return hash;
    }


    private int computeLength() {
        int length = 0;
        if ((separators & HAS_LEADING) != 0) {
            length++;
        }
        //add the segment lengths
        int max = segments.length;
        if (max > 0) {
            for (int i = 0; i < max; i++) {
                length += segments[i].length();
            }
            //add the separator lengths
            length += max - 1;
        }
        if ((separators & HAS_TRAILING) != 0) {
            length++;
        }
        return length;
    }

    private static int computeSegmentCount(String path) {
        int len = path.length();
        if (len == 0 || (len == 1 && path.charAt(0) == SEPARATOR)) {
            return 0;
        }
        int count = 1;
        int prev = -1;
        int i;
        while ((i = path.indexOf(SEPARATOR, prev + 1)) != -1) {
            if (i != prev + 1 && i != len) {
                ++count;
            }
            prev = i;
        }
        if (path.charAt(len - 1) == SEPARATOR) {
            --count;
        }
        return count;
    }

    /**
     * Computes the segment array for the given canonicalized path.
     */
    private static String[] computeSegments(String path) {
        // performance sensitive --- avoid creating garbage
        int segmentCount = computeSegmentCount(path);
        if (segmentCount == 0) {
            return NO_SEGMENTS;
        }
        String[] newSegments = new String[segmentCount];
        int len = path.length();
        // check for initial slash
        int firstPosition = (path.charAt(0) == SEPARATOR) ? 1 : 0;
        // check for UNC
        if (firstPosition == 1 && len > 1 && (path.charAt(1) == SEPARATOR)) {
            firstPosition = 2;
        }
        int lastPosition = (path.charAt(len - 1) != SEPARATOR) ? len - 1 : len - 2;
        // for non-empty paths, the number of segments is
        // the number of slashes plus 1, ignoring any leading
        // and trailing slashes
        int next = firstPosition;
        for (int i = 0; i < segmentCount; i++) {
            int start = next;
            int end = path.indexOf(SEPARATOR, next);
            if (end == -1) {
                newSegments[i] = path.substring(start, lastPosition + 1);
            } else {
                newSegments[i] = path.substring(start, end);
            }
            next = end + 1;
        }
        return newSegments;
    }

    /* (Intentionally not included in javadoc)
     * Compares objects for equality.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Path)) {
            return false;
        }
        Path target = (Path) obj;
        //check leading separators and hash code
        if ((separators & HASH_MASK) != (target.separators & HASH_MASK)) {
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

    /* (Intentionally not included in javadoc)
     * @see IPath#getFileExtension
     */
    public String getFileExtension() {
        if (hasTrailingSeparator()) {
            return null;
        }
        String lastSegment = lastSegment();
        if (lastSegment == null) {
            return null;
        }
        int index = lastSegment.lastIndexOf('.');
        if (index == -1) {
            return null;
        }
        return lastSegment.substring(index + 1);
    }

    /* (Intentionally not included in javadoc)
     * Computes the hash code for this object.
     */
    @Override
    public int hashCode() {
        return separators & HASH_MASK;
    }

    /* (Intentionally not included in javadoc)
     * @see IPath#hasTrailingSeparator2
     */
    public boolean hasTrailingSeparator() {
        return (separators & HAS_TRAILING) != 0;
    }

    /**
     * Initializes the current path with the given string.
     */
    private Path initialize(String path) {
        assert path != null;

        path = collapseSlashes(path);
        int len = path.length();

        //compute the separators array
        if (len < 2) {
            if (len == 1 && path.charAt(0) == SEPARATOR) {
                separators = HAS_LEADING;
            } else {
                separators = 0;
            }
        } else {
            boolean hasLeading = path.charAt(0) == SEPARATOR;
            boolean hasTrailing = path.charAt(len - 1) == SEPARATOR;
            separators = hasLeading ? HAS_LEADING : 0;
            if (hasTrailing) {
                separators |= HAS_TRAILING;
            }
        }
        //compute segments and ensure canonical form
        segments = computeSegments(path);
        if (!canonicalize()) {
            //compute hash now because canonicalize didn't need to do it
            separators = (separators & ALL_SEPARATORS) | (computeHashCode() << USED_BITS);
        }
        return this;
    }

    public boolean isAbsolute() {
        //it's absolute if it has a leading separator
        return (separators & HAS_LEADING) != 0;
    }

    public boolean isEmpty() {
        //true if no segments and no leading prefix
        return segments.length == 0 && ((separators & ALL_SEPARATORS) != HAS_LEADING);
    }

    public boolean isPrefixOf(Path anotherPath) {
        if (isEmpty() || (isRoot() && anotherPath.isAbsolute())) {
            return true;
        }
        int len = segments.length;
        if (len > anotherPath.segmentCount()) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            if (!segments[i].equals(anotherPath.segment(i))) {
                return false;
            }
        }
        return true;
    }

    public boolean isRoot() {
        //must have no segments, a leading separator, and not be a UNC path.
        return this == ROOT || (segments.length == 0 && ((separators & ALL_SEPARATORS) == HAS_LEADING));
    }

    public static boolean isValidPath(String path) {
        Path test = new Path(path);
        for (int i = 0, max = test.segmentCount(); i < max; i++) {
            if (!isValidSegment(test.segment(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidSegment(String segment) {
        int size = segment.length();
        if (size == 0) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            char c = segment.charAt(i);
            if (c == '/') {
                return false;
            }
        }
        return true;
    }

    /* (Intentionally not included in javadoc)
     * @see IPath#lastSegment()
     */
    public String lastSegment() {
        int len = segments.length;
        return len == 0 ? null : segments[len - 1];
    }

    public Path makeAbsolute() {
        if (isAbsolute()) {
            return this;
        }
        Path result = new Path(segments, separators | HAS_LEADING);
        //may need canonicalizing if it has leading ".." or "." segments
        if (result.segmentCount() > 0) {
            String first = result.segment(0);
            if (first.equals("..") || first.equals(".")) { //$NON-NLS-1$ //$NON-NLS-2$
                result.canonicalize();
            }
        }
        return result;
    }

    public Path makeRelative() {
        if (!isAbsolute()) {
            return this;
        }
        return new Path(segments, separators & HAS_TRAILING);
    }

    public int matchingFirstSegments(Path anotherPath) {
        assert anotherPath != null;
        int anotherPathLen = anotherPath.segmentCount();
        int max = Math.min(segments.length, anotherPathLen);
        int count = 0;
        for (int i = 0; i < max; i++) {
            if (!segments[i].equals(anotherPath.segment(i))) {
                return count;
            }
            count++;
        }
        return count;
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

    public Path removeFirstSegments(int count) {
        if (count == 0) {
            return this;
        }
        if (count >= segments.length) {
            return new Path(NO_SEGMENTS, 0);
        }
        assert count > 0;
        int newSize = segments.length - count;
        String[] newSegments = new String[newSize];
        System.arraycopy(segments, count, newSegments, 0, newSize);

        //result is always a relative path
        return new Path(newSegments, separators & HAS_TRAILING);
    }

    public Path removeLastSegments(final int count) {
        if (count == 0) {
            return this;
        }
        if (count >= segments.length) {
            //result will have no trailing separator
            return new Path(NO_SEGMENTS, separators & HAS_LEADING);
        }
        assert count > 0;
        final int newSize = segments.length - count;
        final String[] newSegments = new String[newSize];
        System.arraycopy(segments, 0, newSegments, 0, newSize);
        return new Path(newSegments, separators);
    }

    public Path removeTrailingSeparator() {
        if (!hasTrailingSeparator()) {
            return this;
        }
        return new Path(segments, separators & HAS_LEADING);
    }

    public String segment(int index) {
        if (index >= segments.length) {
            return null;
        }
        return segments[index];
    }

    public int segmentCount() {
        return segments.length;
    }

    public String[] segments() {
        final String[] segmentCopy = new String[segments.length];
        System.arraycopy(segments, 0, segmentCopy, 0, segments.length);
        return segmentCopy;
    }

    @Override
    public String toString() {
        final int resultSize = computeLength();
        if (resultSize <= 0) {
            return EMPTY_STRING;
        }
        char[] result = new char[resultSize];
        int offset = 0;
        if ((separators & HAS_LEADING) != 0) {
            result[offset++] = SEPARATOR;
        }
        final int len = segments.length - 1;
        if (len >= 0) {
            //append all but the last segment, with separators
            for (int i = 0; i < len; i++) {
                final int size = segments[i].length();
                segments[i].getChars(0, size, result, offset);
                offset += size;
                result[offset++] = SEPARATOR;
            }
            //append the last segment
            final int size = segments[len].length();
            segments[len].getChars(0, size, result, offset);
            offset += size;
        }
        if ((separators & HAS_TRAILING) != 0) {
            result[offset] = SEPARATOR;
        }
        return new String(result);
    }

    public Path uptoSegment(final int count) {
        if (count == 0) {
            return new Path(NO_SEGMENTS, separators & HAS_LEADING);
        }
        if (count >= segments.length) {
            return this;
        }
        assert count > 0; // Invalid parameter to Path.uptoSegment
        final String[] newSegments = new String[count];
        System.arraycopy(segments, 0, newSegments, 0, count);
        return new Path(newSegments, separators);
    }

    /**
     * Gets the name of the icon file so that it can be displayed as alt text.
     *
     * @param iconPath
     * @return
     */
    public static String getFileNameFromPath(String iconPath) {
        String iconName;
        //        String fileSeparator = System.getProperty("file.separator");

        //temporary not working with the file separator, only with /
        int firstCharOfIconName = iconPath.lastIndexOf(SEPARATOR);
        int lastCharOfIconName = iconPath.lastIndexOf(".");
        if (firstCharOfIconName == -1) {
            iconName = iconPath;
        } else {
            iconName = iconPath.substring(firstCharOfIconName,
                    lastCharOfIconName);
        }
        return iconName;
    }

}
