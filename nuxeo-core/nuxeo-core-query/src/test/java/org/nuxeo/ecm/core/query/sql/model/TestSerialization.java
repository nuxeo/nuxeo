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

import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.common.collections.SerializableArrayMap;
import org.nuxeo.common.utils.SerializableHelper;
import org.nuxeo.ecm.core.query.sql.SQLQueryParser;

/**
 * Test that serialization of SQLQuery instances actually works.
 * <p>
 * This test case certainly doesn't cover all neeeded aspects. One should also
 * be concerned that standard serialization might be inefficient in terms of
 * weight for such object and rely on something else if possible.
 *
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 *
 */
public class TestSerialization {

    public static void check(String query) {
        SQLQuery sql = SQLQueryParser.parse(query);
        assertTrue(SerializableHelper.isSerializable(sql));
    }

    @Test
    public void testQuery1() {
        check("SELECT dc:title, dc:dublincore FROM Document "
                + "WHERE dc:created > TIMESTAMP '2007-02-03 0:0'");
    }

    @Test
    public void testQuery2() {
        check("SELECT * FROM Document ");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSerializableArrayMap() throws Exception {
        SerializableArrayMap<String, Operand> elements = new SerializableArrayMap<String, Operand>();
        elements.put("abc", new StringLiteral("table"));
        assertEquals(1, elements.size());

        SerializableArrayMap<String, Operand> elements2
                = (SerializableArrayMap<String, Operand>) SerializableHelper.serializeUnserialize(elements);
        assertEquals(1, elements2.size());
    }

    @Test
    public void testSelect() throws Exception {
        SelectList elements = new SelectList();
        elements.put("abc", new StringLiteral("table"));
        assertEquals(1, elements.size());

        assertTrue(SerializableHelper.isSerializable(new StringLiteral("x")));

        assertTrue(SerializableHelper.isSerializable(elements));
        assertTrue(SerializableHelper.isSerializable(new SelectClause(elements)));

        SelectList elements2 = (SelectList) SerializableHelper.serializeUnserialize(elements);
        assertEquals(1, elements2.size());
    }

    @Test
    public void testFrom() throws Exception {
        FromList elements = new FromList();
        elements.put("abc", "ABC");

        assertTrue(SerializableHelper.isSerializable(elements));
        assertTrue(SerializableHelper.isSerializable(new FromClause(elements)));

        FromList elements2 = (FromList) SerializableHelper.serializeUnserialize(elements);
        assertEquals(1, elements2.size());
    }

    @Test
    public void testSQLQUery() throws Exception {

        String queryString = "SELECT * FROM Document WHERE kw1='vie' AND kw2='mechante'";
        SQLQuery parsed = SQLQueryParser.parse(queryString);
        assertEquals(
                "SELECT * FROM Document WHERE kw1='vie' AND kw2='mechante'",
                parsed.toString());
        assertEquals(0, parsed.getSelectClause().elements.size());
        assertEquals(1, parsed.getFromClause().elements.size());

        SQLQuery dumped = (SQLQuery) SerializableHelper.serializeUnserialize(parsed);
        assertEquals(
                "SELECT * FROM Document WHERE kw1='vie' AND kw2='mechante'",
                dumped.toString());
        assertEquals(0, dumped.getSelectClause().elements.size());
        assertEquals(1, dumped.getFromClause().elements.size());

    }

}
