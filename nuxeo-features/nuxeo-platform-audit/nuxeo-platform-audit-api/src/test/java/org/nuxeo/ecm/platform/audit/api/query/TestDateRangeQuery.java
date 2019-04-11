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
 *     anguenot
 *
 * $Id: TestDateRangeQuery.java 21908 2007-07-04 09:34:14Z janguenot $
 */

package org.nuxeo.ecm.platform.audit.api.query;

import java.util.Date;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test date range query.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestDateRangeQuery {

    @Test
    public void testOneWithMinutesOnly() throws AuditQueryException {
        Date now = new Date();
        Date then = DateRangeParser.parseDateRangeQuery(now, "2m");
        assertTrue(then.before(now));

        // 2 minutes in miliseconds
        long expected = 2 * 60 * 1000L;
        assertEquals(expected, now.getTime() - then.getTime());

        then = DateRangeParser.parseDateRangeQuery(now, "50m");
        assertTrue(then.before(now));

        // 50 minutes in miliseconds
        expected = 50 * 60 * 1000L;
        assertEquals(expected, now.getTime() - then.getTime());
    }

    @Test
    public void testOneWithHoursOnly() throws AuditQueryException {
        Date now = new Date();

        Date then = DateRangeParser.parseDateRangeQuery(now, "2h");
        assertTrue(then.before(now));

        // 2 hours in miliseconds
        long expected = 2 * 60 * 60 * 1000L;
        assertEquals(expected, now.getTime() - then.getTime());

        then = DateRangeParser.parseDateRangeQuery(now, "24h");
        assertTrue(then.before(now));

        // 24 hours in miliseconds
        expected = 24 * 60 * 60 * 1000L;
        assertEquals(expected, now.getTime() - then.getTime());
    }

    @Test
    public void testWrongFormat() {
        boolean raises = false;
        Date now = new Date();
        try {
            DateRangeParser.parseDateRangeQuery(now, "xh");
        } catch (AuditQueryException e) {
            raises = true;
        }
        assertTrue(raises);
    }

    @Test
    public void testWrongFormatOrder() {
        boolean raises = false;
        Date now = new Date();
        try {
            DateRangeParser.parseDateRangeQuery(now, "2h2m");
        } catch (AuditQueryException e) {
            raises = true;
        }
        assertTrue(raises);
    }

    @Test
    public void testMixing() throws AuditQueryException {
        Date now = new Date();
        Date then = DateRangeParser.parseDateRangeQuery(now, " 2m 2h ");

        // 2 hours and 2 minutes in miliseconds
        long expected = ((2L * 60 + 2) * 60) * 1000;
        assertEquals(expected, now.getTime() - then.getTime());
    }

}
