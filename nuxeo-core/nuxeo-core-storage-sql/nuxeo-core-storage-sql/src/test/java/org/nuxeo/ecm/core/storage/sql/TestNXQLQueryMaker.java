/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.sql;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.core.storage.sql.jdbc.NXQLQueryMaker.canonicalXPath;
import static org.nuxeo.ecm.core.storage.sql.jdbc.NXQLQueryMaker.simpleXPath;

import org.junit.Test;

public class TestNXQLQueryMaker {

    @Test
    public void testCanonicalXPath() throws Exception {
        assertEquals("abc", canonicalXPath("abc"));
        assertEquals("abc/def", canonicalXPath("abc/def"));
        assertEquals("abc/5", canonicalXPath("abc/def[5]"));
        assertEquals("abc/5/ghi", canonicalXPath("abc/def[5]/ghi"));
    }

    @Test
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
