/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.ra;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

/**
 * @author Florent Guillaume
 */
public class TestParseProperties extends TestCase {

    public static Map<String, String> parse(String expr) {
        return ManagedConnectionFactoryImpl.parseProperties(expr);
    }

    public void test() throws Exception {
        Map<String, String> props = new HashMap<String, String>();
        assertEquals(props, parse(""));

        String expr = "key1=val1";
        props.put("key1", "val1");
        assertEquals(props, parse(expr));
        expr += ";";
        assertEquals(props, parse(expr));

        expr += "key2=val2";
        props.put("key2", "val2");
        assertEquals(props, parse(expr));
        expr += ";";
        assertEquals(props, parse(expr));

        expr += "key3=a=b;;c===d;;;;e=f";
        props.put("key3", "a=b;c===d;;e=f");
        assertEquals(props, parse(expr));
    }
}
