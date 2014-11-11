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

package org.nuxeo.ecm.platform.relations.api.impl;

import java.util.Calendar;
import java.util.Date;

import junit.framework.TestCase;

import org.nuxeo.ecm.platform.relations.api.Literal;

/**
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 *
 */
public class TestRelationDate extends TestCase {

    private final Literal literal = new LiteralImpl("2012-11-23T03:57:01");

    public void testDateRoundTrip() {
        Date date = RelationDate.getDate(literal);
        Literal newLit = RelationDate.getLiteralDate(date);
        assertEquals(newLit, literal);
    }

    public void testCalendarRoundTrip() {
        Calendar cal = RelationDate.getCalendar(literal);
        Literal newLit = RelationDate.getLiteralDate(cal);
        assertEquals(newLit, literal);
    }

    /** Uses date variant to prove that the calendar one works.
     */
    @SuppressWarnings("deprecation")
    public void testCalendarDate() {
        Date date = new Date(2007, 7, 6, 1, 23, 15);
        Literal newLit = RelationDate.getLiteralDate(date);
        Calendar cal = RelationDate.getCalendar(newLit);
        assertEquals(date, cal.getTime());
    }

    public void testWrongLiteralCalendar() {
        assertNull(RelationDate.getCalendar(new LiteralImpl("qwqe")));
    }

}
