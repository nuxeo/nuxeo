/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs.servlet.mapping;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PathParser {

    protected String[] array;
    protected int count;

    protected char[] buf;
    protected int bufCount;

    public PathParser() {
        reset();
    }

    public void reset() {
        array = new String[16];
        buf = new char[16];
        count = 0;
        bufCount = 0;
    }

    public Path parse(String path) {
        return parse(path, -1);
    }

    public Path parse(String path, int userBits) {
        char[] chars = path.toCharArray();
        if (chars.length == 0) {
            return Path.EMPTY;
        }
        if (chars.length == 1 && chars[0] == '/') {
            return Path.ROOT;
        }

        int i = 0;
        int len = chars.length;
        int bits = 0;
        if (chars[chars.length-1] == '/') {
            bits |= Path.HAS_TRAILING_SLASH;
            len--;
        }
        if (chars[0] == '/') {
            bits |= Path.HAS_LEADING_SLASH;
            i++;
        }

        for (; i<len; i++) {
            char c = chars[i];
            if (c == '/') {
                if (hasSegment()) {
                    addSegment(currentSegment());
                    resetBuf();
                } // else -> duplicate / - it will be ignored
            } else {
                append(c);
            }
        }
        if (hasSegment()) {
            addSegment(currentSegment());
        }

        return new Path(getSegments(), userBits == -1 ? bits : userBits);
    }


    public void back() {
        if (count == 0) {
            add("..");
        } else {
            count--;
        }
    }

    public void addSegment(String segment) {
        if ("..".equals(segment)) {
            back();
        } else if (!".".equals(segment)) {
            add(segment);
        }
    }

    public String[] getSegments() {
        String[] result = new String[count];
        System.arraycopy(array, 0, result, 0, count);
        return result;
    }


    private final void add(String segment) {
        if (count + 1 == array.length) {
            String[] result = new String[count+16];
            System.arraycopy(array, 0, result, 0, count);
            array = result;
        }
        array[count++] = segment;
    }

    private final void append(char c) {
        if (bufCount + 1 == buf.length) {
            char[] result = new char[bufCount+16];
            System.arraycopy(buf, 0, result, 0, bufCount);
            buf = result;
        }
        buf[bufCount++] = c;
    }

    private final String currentSegment() {
        return new String(buf, 0, bufCount);
    }

    private final boolean hasSegment() {
        return bufCount > 0;
    }

    private final void resetBuf() {
        bufCount = 0;
    }

}
