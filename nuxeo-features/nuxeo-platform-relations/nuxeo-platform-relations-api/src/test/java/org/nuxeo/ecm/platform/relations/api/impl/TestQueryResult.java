/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: TestQueryResult.java 22853 2007-07-22 21:09:50Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.api.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QueryResult;

public class TestQueryResult extends TestCase {

    public void testInit() {
        List<String> variableNames = new ArrayList<String>();
        variableNames.add("subject");
        variableNames.add("object");

        List<Map<String, Node>> results = new ArrayList<Map<String, Node>>();
        Map<String, Node> res1 = new HashMap<String, Node>();
        res1.put("subject", new ResourceImpl("http://toto"));
        res1.put("object", new ResourceImpl("http://titi"));
        results.add(res1);

        Map<String, Node> res2 = new HashMap<String, Node>();
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
