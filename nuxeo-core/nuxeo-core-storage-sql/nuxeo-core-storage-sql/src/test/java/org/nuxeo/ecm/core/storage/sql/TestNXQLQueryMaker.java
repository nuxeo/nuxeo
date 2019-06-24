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
import static org.nuxeo.ecm.core.query.sql.NXQL.addPeriondAndDuration;
import static org.nuxeo.ecm.core.storage.sql.jdbc.NXQLQueryMaker.canonicalXPath;
import static org.nuxeo.ecm.core.storage.sql.jdbc.NXQLQueryMaker.simpleXPath;

import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
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

    @Test
    public void testNowPeriodAndDuration() throws Exception {
        DateTime dateTime = new DateTime(2001, 2, 3, 4, 5, 6, 7, ISOChronology.getInstanceUTC());
        assertEquals("2001-02-03T04:05:06.007Z", dateTime.toString());
        //
        DateTime d1 = addPeriondAndDuration(dateTime, "P1Y2M3DT4H5M6S");
        assertEquals("2002-04-06T08:10:12.007Z", d1.toString());
        DateTime d2 = addPeriondAndDuration(dateTime, "P26D");
        assertEquals("2001-03-01T04:05:06.007Z", d2.toString());
        DateTime d3 = addPeriondAndDuration(dateTime, "-P1M");
        assertEquals("2001-01-03T04:05:06.007Z", d3.toString());
        DateTime d4 = addPeriondAndDuration(dateTime, "PT20H");
        assertEquals("2001-02-04T00:05:06.007Z", d4.toString());
    }

}
