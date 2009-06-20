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
 */
package org.nuxeo.build.maven.filter;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class SegmentMatch {

    public static final SegmentMatch ANY = new SegmentMatch() {
        public boolean match(String arg0) {
            return true;
        }
    };

    abstract boolean match(String segment);

    public static SegmentMatch parse(String pattern) {
        int p = pattern.indexOf("*");
        if (p == -1) {
            return new ExactMatch(pattern);
        }
        int len = pattern.length();
        if (len == 0) {
            return SegmentMatch.ANY;
        }
        if (p == len-1) {
            return new PrefixMatch(pattern.substring(0, pattern.length()-1));
        }
        if (p == 0) {
            return new SuffixMatch(pattern.substring(1));
        }
        return new MiddleMatch(pattern.substring(0, p), pattern.substring(p+1));
    }

}
