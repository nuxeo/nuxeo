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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.test;

import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.platform.rendering.wiki.extensions.PatternFilter;

public class TestPatternFilter {

    @Test
    public void test1() {
        PatternFilter filter = new PatternFilter("[A-Z]+[a-z]+[A-Z][A-Za-z]*", "<link>$0</link>");
        assertEquals("<link>MyName</link>", filter.apply("MyName"));
    }

    @Test
    public void test2() {
        PatternFilter filter = new PatternFilter("NXP-[0-9]+", "<a href=\"http://jira.nuxeo.org/browse/$0\">$0</a>");
        assertEquals("<a href=\"http://jira.nuxeo.org/browse/NXP-1234\">NXP-1234</a>", filter.apply("NXP-1234"));
        assertNull(filter.apply("NXP1234"));
    }

}
