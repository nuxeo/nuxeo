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

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class WildcardSegmentMatcher extends SegmentMatcher {

    protected final char[] pattern;

    public WildcardSegmentMatcher(String pattern) {
        this.pattern = pattern.replace("**", "*").toCharArray();
    }

    public WildcardSegmentMatcher(char[] pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean matches(String segment) {
        if (pattern.length == 0) {
            return true;
        }
        return matches(segment.toCharArray(), 0, 0);
    }

    public boolean matches(char[] segment, int soff, int poff) {
        if (poff == pattern.length) {
            // pattern consumed
            if (soff == segment.length) {
                // segment consumed too
                return true;
            }
            // segment is not yet consumed
            if (pattern[pattern.length - 1] == '*') {
                // last char is a wildcard => matched
                return true;
            }
            return false;
        }
        if (soff == segment.length) {
            // segment consumed but pattern is not yet consumed
            if (poff + 1 == pattern.length && pattern[poff] == '*') {
                // there is only one char remaining and it is a wildcard => matched
                return true;
            }
            return false;
        }

        if (pattern[poff] == '*') {
            // current pattern char is a wildcard - try all substrings
            for (int i = soff; i < segment.length; i++) {
                if (matches(segment, i, poff + 1)) {
                    return true;
                }
            }
            return false;
        }

        // test is the current char is matching
        if (pattern[poff] != '?' && pattern[poff] != segment[soff]) {
            return false; // not matching
        }

        // continue iteration on segments and matchers
        return matches(segment, soff + 1, poff + 1);

    }

    @Override
    public String toString() {
        return new String(pattern);
    }
}
