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

package org.nuxeo.ecm.core.query.sql.model;

import junit.framework.TestCase;

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
public class TestSerialization extends TestCase {

    public static void check(String query) {
        SQLQuery sql = SQLQueryParser.parse(query);
        assertTrue(SerializableHelper.isSerializable(sql));
    }

    public void testQuery1() {
        check("SELECT dc:title, dc:dublincore FROM Document "
                + "WHERE dc:created > TIMESTAMP '2007-02-03 0:0'");
    }

    public void testQuery2() {
        check("SELECT * FROM Document ");
    }

    @SuppressWarnings("unchecked")
    public void testSerializableArrayMap() throws Exception {
        SerializableArrayMap<String, Operand> elements = new SerializableArrayMap<String, Operand>();
        elements.put("abc", new StringLiteral("table"));
        assertEquals(1, elements.size());

        SerializableArrayMap<String, Operand> elements2
                = (SerializableArrayMap<String, Operand>) SerializableHelper.serializeUnserialize(elements);
        assertEquals(1, elements2.size());
    }

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

    public void testFrom() throws Exception {
        FromList elements = new FromList();
        elements.put("abc", "ABC");

        assertTrue(SerializableHelper.isSerializable(elements));
        assertTrue(SerializableHelper.isSerializable(new FromClause(elements)));

        FromList elements2 = (FromList) SerializableHelper.serializeUnserialize(elements);
        assertEquals(1, elements2.size());
    }

    public void testSQLQUery() throws Exception {

        String queryString = "SELECT * FROM Document WHERE kw1='vie' AND kw2='mechante'";
        SQLQuery parsed = SQLQueryParser.parse(queryString);
        assertEquals(
                "SELECT * FROM Document WHERE kw1 = 'vie' AND kw2 = 'mechante'",
                parsed.toString());
        assertEquals(0, parsed.getSelectClause().elements.size());
        assertEquals(1, parsed.getFromClause().elements.size());

        SQLQuery dumped = (SQLQuery) SerializableHelper.serializeUnserialize(parsed);
        assertEquals(
                "SELECT * FROM Document WHERE kw1 = 'vie' AND kw2 = 'mechante'",
                dumped.toString());
        assertEquals(0, dumped.getSelectClause().elements.size());
        assertEquals(1, dumped.getFromClause().elements.size());

    }

}
