/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.webengine.util;

import java.util.regex.Pattern;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PathMatcher {

    public static PathMatcher getRegexMatcher(String regex) {
        return new RegexMatcher(regex);
    }

    public static PathMatcher getPrefixMatcher(String prefix) {
        return new PrefixMatcher(prefix);
    }

    public static PathMatcher getAllMatcher() {
        return new PathMatcher();
    }

    protected PathMatcher() {
    }

    public boolean match(String value) {
        return true;
    }

    public static class RegexMatcher extends PathMatcher {
        protected Pattern pattern;

        public RegexMatcher(String regex) {
            pattern = Pattern.compile(regex);
        }

        @Override
        public boolean match(String value) {
            return pattern.matcher(value).matches();
        }
    }

    public static class PrefixMatcher extends PathMatcher {
        protected String prefix;

        public PrefixMatcher(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public boolean match(String value) {
            return value.startsWith(prefix);
        }
    }

}
