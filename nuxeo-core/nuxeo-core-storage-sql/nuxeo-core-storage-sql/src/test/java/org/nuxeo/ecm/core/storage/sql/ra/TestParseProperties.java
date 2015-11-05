/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.ra;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Florent Guillaume
 */
public class TestParseProperties {

    public static Map<String, String> parse(String expr) {
        return ManagedConnectionFactoryImpl.parseProperties(expr);
    }

    @Test
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
