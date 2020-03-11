/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
        Map<String, String> props = new HashMap<>();
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
