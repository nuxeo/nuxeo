/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: TestQueryResult.java 22853 2007-07-22 21:09:50Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.api.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QueryResult;

public class TestQueryResult {

    @Test
    public void testInit() {
        List<String> variableNames = new ArrayList<>();
        variableNames.add("subject");
        variableNames.add("object");

        List<Map<String, Node>> results = new ArrayList<>();
        Map<String, Node> res1 = new HashMap<>();
        res1.put("subject", new ResourceImpl("http://toto"));
        res1.put("object", new ResourceImpl("http://titi"));
        results.add(res1);

        Map<String, Node> res2 = new HashMap<>();
        res2.put("subject", new ResourceImpl("http://toto"));
        res2.put("object", new LiteralImpl("lalala"));
        results.add(res2);

        Integer count = 2;
        QueryResult qr = new QueryResultImpl(count, variableNames, results);
        assertSame(qr.getCount(), 2);
        assertEquals(variableNames, qr.getVariableNames());
        assertEquals(results, qr.getResults());
    }

}
