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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.ecm.core.query.sql.model;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:fg@nuxeo.com">Florent Guillaume</a>
 */
public class TestLiterals {

    @Test
    public void testStringLiteral() {
        StringLiteral l;
        l = new StringLiteral("foo");
        assertEquals("'foo'", l.toString());
    }

    @Test
    public void testIntegerLiteral() {
        IntegerLiteral l;
        l = new IntegerLiteral("123");
        assertEquals("123", l.toString());
        l = new IntegerLiteral("-123");
        assertEquals("-123", l.toString());
    }

    @Test
    public void testDoubleLiteral() {
        DoubleLiteral l;
        l = new DoubleLiteral("1.23");
        assertEquals("1.23", l.toString());
        l = new DoubleLiteral(".2");
        assertEquals("0.2", l.toString());
    }

    @Test
    public void testDateLiteral() {
        DateLiteral d;
        d = new DateLiteral("2007-01-02", true);
        assertEquals("DATE '2007-01-02'", d.toString());
        d = new DateLiteral("2007-01-30T01:02:03+04:56", false); // with T
        assertEquals("TIMESTAMP '2007-01-29T20:06:03.000Z'", d.toString());
        d = new DateLiteral("2007-01-30 01:02:03+04:56", false); // with space
        assertEquals("TIMESTAMP '2007-01-29T20:06:03.000Z'", d.toString());
    }

}
