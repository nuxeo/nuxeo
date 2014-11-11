/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     anguenot
 *
 * $Id: TestDateRangeQuery.java 21908 2007-07-04 09:34:14Z janguenot $
 */

package org.nuxeo.ecm.platform.audit.api.query;

import java.util.Date;

import junit.framework.TestCase;

/**
 * Test date range query.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class TestDateRangeQuery extends TestCase {

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

    public void testWrongFormat() throws AuditQueryException {
        boolean raises = false;
        Date now = new Date();
        try {
            DateRangeParser.parseDateRangeQuery(now, "xh");
        } catch (AuditQueryException e) {
            raises = true;
        }
        assertTrue(raises);
    }

    public void testWrongFormatOrder() throws AuditQueryException {
        boolean raises = false;
        Date now = new Date();
        try {
            DateRangeParser.parseDateRangeQuery(now, "2h2m");
        } catch (AuditQueryException e) {
            raises = true;
        }
        assertTrue(raises);
    }

    public void testMixing() throws AuditQueryException {
        Date now = new Date();
        Date then = DateRangeParser.parseDateRangeQuery(now, " 2m 2h ");

        // 2 hours and 2 minutes in miliseconds
        long expected = ((2L * 60 + 2) * 60) * 1000;
        assertEquals(expected, now.getTime() - then.getTime());
    }

}
