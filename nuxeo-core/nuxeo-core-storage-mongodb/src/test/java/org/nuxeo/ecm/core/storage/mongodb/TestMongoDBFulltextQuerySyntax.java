/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.mongodb;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestMongoDBFulltextQuerySyntax {

    protected void assertDialectFT(String expected, String query) {
        assertEquals(expected,
                MongoDBQueryBuilder.getMongoDBFulltextQuery(query));
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
        assertDialectFT("\"foo bar\" \"baz\" \"gee man\"",
                "\"foo bar\" baz OR \"gee man\"");
        assertDialectFT("\"foo bar\" -\"gee man\"", "\"foo bar\" -\"gee man\"");
        // no prefix syntax in MongoDB
        // assertDialectFT("\"foo*\"", "foo*");
        // assertDialectFT("\"foo\" \"bar*\")", "foo bar*");
    }

}
