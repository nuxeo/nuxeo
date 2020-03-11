/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.webengine.util;

import java.util.regex.Pattern;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
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
