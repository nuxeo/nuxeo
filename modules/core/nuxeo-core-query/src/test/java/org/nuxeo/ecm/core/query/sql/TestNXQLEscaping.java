/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
        assertEquals("a\\nb", NXQL.escapeStringInner("a\nb"));

        assertEquals("''", NXQL.escapeString(""));
        assertEquals("'a\\'b'", NXQL.escapeString("a'b"));
        assertEquals("'a\\\\b'", NXQL.escapeString("a\\b"));
        assertEquals("'a\\\\\\'b'", NXQL.escapeString("a\\'b"));
        assertEquals("'a\\nb'", NXQL.escapeString("a\nb"));
    }

}
