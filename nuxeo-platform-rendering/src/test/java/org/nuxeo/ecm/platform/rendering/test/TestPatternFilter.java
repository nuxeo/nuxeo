/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
