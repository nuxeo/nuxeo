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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.test;

import junit.framework.TestCase;

import org.nuxeo.ecm.platform.rendering.wiki.extensions.PatternFilter;

public class TestPatternFilter extends TestCase {

    public void test1() {
        PatternFilter filter = new PatternFilter("[A-Z]+[a-z]+[A-Z][A-Za-z]*", "<link>$0</link>");
        assertEquals("<link>MyName</link>", filter.apply("MyName"));
    }

    public void test2() {
        PatternFilter filter = new PatternFilter(
                "NXP-[0-9]+", "<a href=\"http://jira.nuxeo.org/browse/$0\">$0</a>");
        assertEquals("<a href=\"http://jira.nuxeo.org/browse/NXP-1234\">NXP-1234</a>",
                filter.apply("NXP-1234"));
        assertNull(filter.apply("NXP1234"));
    }

}
