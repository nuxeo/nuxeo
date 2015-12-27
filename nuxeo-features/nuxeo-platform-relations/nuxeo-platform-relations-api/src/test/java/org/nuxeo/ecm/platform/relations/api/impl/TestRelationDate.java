/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.relations.api.impl;

import java.util.Calendar;
import java.util.Date;

import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.platform.relations.api.Literal;

/**
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 */
public class TestRelationDate {

    private final Literal literal = new LiteralImpl("2012-11-23T03:57:01");

    @Test
    public void testDateRoundTrip() {
        Date date = RelationDate.getDate(literal);
        Literal newLit = RelationDate.getLiteralDate(date);
        assertEquals(newLit, literal);
    }

    @Test
    public void testCalendarRoundTrip() {
        Calendar cal = RelationDate.getCalendar(literal);
        Literal newLit = RelationDate.getLiteralDate(cal);
        assertEquals(newLit, literal);
    }

    /**
     * Uses date variant to prove that the calendar one works.
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testCalendarDate() {
        Date date = new Date(2007, 7, 6, 1, 23, 15);
        Literal newLit = RelationDate.getLiteralDate(date);
        Calendar cal = RelationDate.getCalendar(newLit);
        assertEquals(date, cal.getTime());
    }

    @Test
    public void testWrongLiteralCalendar() {
        assertNull(RelationDate.getCalendar(new LiteralImpl("qwqe")));
    }

}
