/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.ecm.core.query.sql.model;

import junit.framework.TestCase;


/**
 * @author <a href="mailto:fg@nuxeo.com">Florent Guillaume</a>
 */
public class TestLiterals extends TestCase {

    public void testStringLiteral() {
        StringLiteral l;
        l = new StringLiteral("foo");
        assertEquals("'foo'", l.toString());
    }

    public void testIntegerLiteral() {
        IntegerLiteral l;
        l = new IntegerLiteral("123");
        assertEquals("123", l.toString());
        l = new IntegerLiteral("-123");
        assertEquals("-123", l.toString());
    }

    public void testDoubleLiteral() {
        DoubleLiteral l;
        l = new DoubleLiteral("1.23");
        assertEquals("1.23", l.toString());
        l = new DoubleLiteral(".2");
        assertEquals("0.2", l.toString());
    }

    public void testDateLiteral() {
        DateLiteral d;
        d = new DateLiteral("2007-01-02", true);
        assertEquals("DATE '2007-01-02'", d.toString());
        d = new DateLiteral("2007-01-30T01:02:03+04:56", false); // with T
        assertEquals("TIMESTAMP '2007-01-30T01:02:03.000+04:56'", d.toString());
        d = new DateLiteral("2007-01-30 01:02:03+04:56", false); // with space
        assertEquals("TIMESTAMP '2007-01-30T01:02:03.000+04:56'", d.toString());
    }

}
