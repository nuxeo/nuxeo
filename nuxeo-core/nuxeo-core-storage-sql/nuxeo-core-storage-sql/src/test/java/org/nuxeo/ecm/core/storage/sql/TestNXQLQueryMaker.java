/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import static org.nuxeo.ecm.core.storage.sql.jdbc.NXQLQueryMaker.simpleXPath;
import static org.nuxeo.ecm.core.storage.sql.jdbc.NXQLQueryMaker.canonicalXPath;
import junit.framework.TestCase;

public class TestNXQLQueryMaker extends TestCase {

    public void testCanonicalXPath() throws Exception {
        assertEquals("abc", canonicalXPath("abc"));
        assertEquals("abc/def", canonicalXPath("abc/def"));
        assertEquals("abc/5", canonicalXPath("abc/def[5]"));
        assertEquals("abc/5/ghi", canonicalXPath("abc/def[5]/ghi"));
    }

    public void testSimpleXPath() throws Exception {
        assertEquals("abc", simpleXPath("abc"));
        assertEquals("abc/def", simpleXPath("abc/def"));
        assertEquals("abc/*", simpleXPath("abc/5"));
        assertEquals("abc/*/def", simpleXPath("abc/5/def"));
        // prop whose name ends with digits
        assertEquals("abc1", simpleXPath("abc1"));
        assertEquals("abc/def1", simpleXPath("abc/def1"));
    }

}
