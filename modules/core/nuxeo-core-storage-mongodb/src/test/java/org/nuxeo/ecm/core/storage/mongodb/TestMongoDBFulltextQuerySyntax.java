/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.mongodb;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestMongoDBFulltextQuerySyntax {

    protected void assertDialectFT(String expected, String query) {
        assertEquals(expected, MongoDBRepositoryQueryBuilder.getMongoDBFulltextQuery(query));
    }

    @Test
    public void testMongoDB() throws Exception {
        assertDialectFT(null, "");
        assertDialectFT("foo", "foo");
        assertDialectFT("foo", "foo :");
        assertDialectFT("foo", "foo :)");
        assertDialectFT("foo", "foo -+-");
        assertDialectFT("\"foo\" \"bar\"", "foo bar");
        assertDialectFT("\"foo\" -\"bar\"", "foo -bar");
        assertDialectFT("\"bar\" -\"foo\"", "-foo bar");
        assertDialectFT("foo bar", "foo OR bar");
        assertDialectFT("foo", "foo OR -bar");
        assertDialectFT("\"foo\" \"bar\" baz", "foo bar OR baz");
        assertDialectFT("\"bar\" -\"foo\" baz", "-foo bar OR baz");
        assertDialectFT("\"foo\" -\"bar\" baz", "foo -bar OR baz");
        assertDialectFT("\"foo bar\"", "\"foo bar\"");
        assertDialectFT("\"foo bar\" \"baz\"", "\"foo bar\" baz");
        assertDialectFT("\"foo bar\" baz", "\"foo bar\" OR baz");
        assertDialectFT("\"foo bar\" \"baz\" \"gee man\"", "\"foo bar\" baz OR \"gee man\"");
        assertDialectFT("\"foo bar\" -\"gee man\"", "\"foo bar\" -\"gee man\"");
        // no prefix syntax in MongoDB
        // assertDialectFT("\"foo*\"", "foo*");
        // assertDialectFT("\"foo\" \"bar*\")", "foo bar*");
    }

}
