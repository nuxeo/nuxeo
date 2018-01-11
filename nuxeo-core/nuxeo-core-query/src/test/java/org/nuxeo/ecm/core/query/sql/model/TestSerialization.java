/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.ecm.core.query.sql.model;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.core.query.sql.SQLQueryParser;

/**
 * Test that serialization of SQLQuery instances actually works.
 * <p>
 * This test case certainly doesn't cover all neeeded aspects. One should also be concerned that standard serialization
 * might be inefficient in terms of weight for such object and rely on something else if possible.
 *
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 */
public class TestSerialization {

    @Test
    public void testQuery1() throws Exception {
        check("SELECT dc:title, dc:dublincore FROM Document " + "WHERE dc:created > TIMESTAMP '2007-02-03 0:0'");
    }

    @Test
    public void testQuery2() throws Exception {
        check("SELECT * FROM Document ");
    }

    @Test
    public void testSerializableHashMap() throws Exception {
        HashMap<String, Operand> elements = new HashMap<>();
        elements.put("abc", new StringLiteral("table"));
        assertEquals(1, elements.size());

        Map<String, Operand> elements2 = serializeUnserialize(elements);
        assertEquals(1, elements2.size());
    }

    @Test
    public void testSelect() throws Exception {
        SelectList elements = new SelectList();
        elements.put("abc", new StringLiteral("table"));
        assertEquals(1, elements.size());

        SelectList elements2 = serializeUnserialize(elements);
        assertEquals(1, elements2.size());
    }

    @Test
    public void testFrom() throws Exception {
        FromList elements = new FromList();
        elements.put("abc", "ABC");

        FromList elements2 = serializeUnserialize(elements);
        assertEquals(1, elements2.size());
    }

    @Test
    public void testSQLQUery() throws Exception {

        String queryString = "SELECT * FROM Document WHERE kw1='vie' AND kw2='mechante'";
        SQLQuery parsed = SQLQueryParser.parse(queryString);
        assertEquals("SELECT * FROM Document WHERE kw1='vie' AND kw2='mechante'", parsed.toString());
        assertEquals(0, parsed.getSelectClause().elements.size());
        assertEquals(1, parsed.getFromClause().elements.size());

        SQLQuery dumped = serializeUnserialize(parsed);
        assertEquals("SELECT * FROM Document WHERE kw1='vie' AND kw2='mechante'", dumped.toString());
        assertEquals(0, dumped.getSelectClause().elements.size());
        assertEquals(1, dumped.getFromClause().elements.size());

    }

    public void check(String query) throws Exception {
        // SQLQuery is serializable by design
        SQLQuery sql = SQLQueryParser.parse(query);
        // just test to serialize and then unserialize it
        SQLQuery sql2 = serializeUnserialize(sql);
        assertEquals(sql, sql2);
    }

    /**
     * Serializes and unserializes back an object to test whether it is correctly rebuilt (to be used in unit tests as
     * sanity checks).
     *
     * @param object the actual object we want to test
     * @return true if the object is serializable.
     */
    @SuppressWarnings("unchecked")
    public <T extends Serializable> T serializeUnserialize(T object) throws Exception {
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        ObjectOutputStream outStream = new ObjectOutputStream(byteOutStream);
        outStream.writeObject(object);
        ByteArrayInputStream byteInStream = new ByteArrayInputStream(byteOutStream.toByteArray());
        ObjectInputStream inStream = new ObjectInputStream(byteInStream);
        return (T) inStream.readObject();
    }

}
