/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.query.sql;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestNXQLEscaping {

    @Test
    public void testEscapeString() throws Exception {
        assertEquals("", NXQL.escapeStringInner(""));
        assertEquals("a\\'b", NXQL.escapeStringInner("a'b"));
        assertEquals("a\\\\b", NXQL.escapeStringInner("a\\b"));
        assertEquals("a\\\\\\'b", NXQL.escapeStringInner("a\\'b"));

        assertEquals("''", NXQL.escapeString(""));
        assertEquals("'a\\'b'", NXQL.escapeString("a'b"));
        assertEquals("'a\\\\b'", NXQL.escapeString("a\\b"));
        assertEquals("'a\\\\\\'b'", NXQL.escapeString("a\\'b"));
    }

}
