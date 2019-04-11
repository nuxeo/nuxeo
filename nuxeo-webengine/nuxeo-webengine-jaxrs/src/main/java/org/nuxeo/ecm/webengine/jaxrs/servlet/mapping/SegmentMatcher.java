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
public abstract class SegmentMatcher {

    public static final SegmentMatcher ANY = new SegmentMatcher() {
        @Override
        public boolean matches(String segment) {
            return true;
        }

        @Override
        public String toString() {
            return "**";
        }
    };

    public static final SegmentMatcher ANY_SEGMENT = new SegmentMatcher() {
        @Override
        public boolean matches(String segment) {
            return true;
        }

        @Override
        public String toString() {
            return "*";
        }
    };

    public abstract boolean matches(String segment);

}
