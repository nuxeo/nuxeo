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

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PathMatcher {

    public static final PathMatcher ANY = new PathMatcher(new SegmentMatcher[0]);

    protected final SegmentMatcher[] matchers;

    public PathMatcher(SegmentMatcher... matchers) {
        this.matchers = matchers;
    }

    public boolean matches(String path) {
        return matches(Path.parse(path));
    }

    public boolean matches(Path path) {
        // if (path.hasTrailingSpace()) {
        // path.append("**");
        // }
        if (matchers.length == 0) {
            return true;
        }
        return matches(path.segments, 0, 0);
    }

    public boolean matches(String[] segments, int soff, int moff) {
        if (soff == segments.length) {
            // segments consumed
            if (moff == matchers.length) {
                // no more matchers => matched
                return true;
            }
            if (moff == matchers.length - 1
                    && matchers[moff] == SegmentMatcher.ANY) {
                // it remains one matcher which is any path => matched
                return true;
            }
            return false;
        }
        if (moff == matchers.length) {
            // no more matchers but segments not consumed
            if (matchers[moff - 1] == SegmentMatcher.ANY) {
                // last matcher is any path
                return true;
            }
            return false;
        }
        SegmentMatcher m = matchers[moff];

        if (m == SegmentMatcher.ANY) {
            // if current matcher is any path try all sub-paths until a match is
            // found
            for (int i = soff; i < segments.length; i++) {
                if (matches(segments, i, moff + 1)) {
                    return true;
                }
            }
            return false;
        }

        // test is the current segment is matching
        if (!m.matches(segments[soff])) {
            return false; // not matching
        }

        // continue iteration on segments and matchers
        return matches(segments, soff + 1, moff + 1);
    }

    public static PathMatcher compile(String path) {
        return compile(Path.parse(path));
    }

    public static PathMatcher compile(Path path) {
        //TODO handle / case
        ArrayList<SegmentMatcher> matchers = new ArrayList<SegmentMatcher>();
        for (String segment : path.segments) {
            if (segment.length() == 0) {
                continue;
            }
            if ("**".equals(segment)) {
                addAnyMatcher(matchers, SegmentMatcher.ANY);
            } else if ("*".equals(segment)) {
                addAnyMatcher(matchers, SegmentMatcher.ANY_SEGMENT);
            } else if (segment.charAt(0) == '('
                    && segment.charAt(segment.length() - 1) == ')') {
                matchers.add(new RegexSegmentMatcher(segment.substring(1,
                        segment.length() - 1)));
            } else {
                matchers.add(createSegmentMatcher(segment));
            }
        }
        return new PathMatcher(
                matchers.toArray(new SegmentMatcher[matchers.size()]));
    }

    private static void addAnyMatcher(List<SegmentMatcher> matchers, SegmentMatcher matcher) {
        if (!matchers.isEmpty() && matchers.get(matchers.size()-1) == matcher) {
            return;
        }
        matchers.add(matcher);
    }

    private static SegmentMatcher createSegmentMatcher(String segment) {
        if (segment.indexOf('*') == -1 && segment.indexOf('?') == -1) {
            return new ExactSegmentMatcher(segment);
        }
        return new WildcardSegmentMatcher(segment);
    }


    @Override
    public String toString() {
        if (matchers.length == 0) {
            return "/**";
        }
        StringBuilder buf = new StringBuilder();
        for (int i=0; i<matchers.length; i++) {
            buf.append("/").append(matchers[i]);
        }
        return buf.toString();
    }
}
