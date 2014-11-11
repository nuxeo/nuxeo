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

package org.nuxeo.ecm.webengine.tests.mapping;

import junit.framework.TestCase;

import org.nuxeo.ecm.webengine.mapping.PathPattern;
import org.nuxeo.ecm.webengine.util.Attributes;

public class TestPathPattern extends TestCase {

    public void testSimple() {
        PathPattern pat = new PathPattern("/a/b/c");
        Attributes m1 = pat.match("/a/b/c");
        assertNotNull(m1);
        assertEquals("/a/b/c", m1.getValue("path"));

        Attributes m2 = pat.match("/a/b/c/d/e");
        assertNull(m2);
    }

    public void testWildcard() {
        PathPattern pat = new PathPattern("/a/b/.*");
        Attributes m1 = pat.match("/a/b/c");
        assertNotNull(m1);
        assertEquals("/a/b/c", m1.getValue("path"));

        Attributes m2 = pat.match("/a/b/c/d/e");
        assertNotNull(m2);
        assertEquals("/a/b/c/d/e", m2.getValue("path"));
    }

    public void testNamedPattern() {
        PathPattern pat = new PathPattern("/(?first:.*)/demos/?");
        Attributes m1 = pat.match("/a/b/demos/");
        assertNotNull(m1);
        assertEquals("/a/b/demos/", m1.getValue("path"));
        assertEquals("a/b", m1.getValue("first"));

        Attributes m2 = pat.match("/foo/demos/bar");
        assertNull(m2);
    }

}
