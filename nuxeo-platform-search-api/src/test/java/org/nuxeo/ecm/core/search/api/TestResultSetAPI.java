/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.search.api;

import junit.framework.TestCase;

import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.ecm.core.search.api.client.search.results.impl.ResultSetImpl;

/**
 * Test result set paging.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class TestResultSetAPI extends TestCase {

    public void testResultSetsPaging() {
        ResultSet set = new ResultSetImpl((SQLQuery) null, null, 0, 9, null, 39, 9);
        assertTrue(set.isFirstPage());

        set = new ResultSetImpl((SQLQuery) null, null, 8, 9, null, 39, 9);
        assertTrue(set.isFirstPage());
        assertTrue(set.hasNextPage());
        assertEquals(1, set.getPageNumber());

        set = new ResultSetImpl((SQLQuery) null, null, 28, 9, null, 39, 9);
        assertFalse(set.isFirstPage());
        assertTrue(set.hasNextPage());
        assertEquals(4, set.getPageNumber());

        set = new ResultSetImpl((SQLQuery) null, null, 32, 9, null, 39, 7);
        assertFalse(set.isFirstPage());
        assertFalse(set.hasNextPage());
        // This is disputable but one can argue that pages
        // are in this case 03-11, 12-21, 22-31, 32-39
        // see also previous test with offset=8, range=9
        assertEquals(4, set.getPageNumber());

        set = new ResultSetImpl((SQLQuery) null, null, 39, 9, null, 39, 1);
        assertFalse(set.isFirstPage());
        assertFalse(set.hasNextPage());
        assertEquals(5, set.getPageNumber());

        set = new ResultSetImpl((SQLQuery) null, null, 9, 9, null, 39, 9);
        assertFalse(set.isFirstPage());
        assertTrue(set.hasNextPage());
        assertEquals(2, set.getPageNumber());

        set = new ResultSetImpl((SQLQuery) null, null, 0, 0, null, 30, 10);
        assertTrue(set.isFirstPage());
        assertFalse(set.hasNextPage());
        assertEquals(1, set.getPageNumber());
    }

    public void testRoundBatches() {
        ResultSet set = new ResultSetImpl((SQLQuery) null, null, 20, 10, null, 37, 10);
        assertFalse(set.isFirstPage());
        assertTrue(set.hasNextPage());
        assertEquals(3, set.getPageNumber());

        set = new ResultSetImpl((SQLQuery) null, null, 30, 10, null, 37, 7);
        assertFalse(set.isFirstPage());
        assertFalse(set.hasNextPage());
        assertEquals(4, set.getPageNumber());
    }

}
