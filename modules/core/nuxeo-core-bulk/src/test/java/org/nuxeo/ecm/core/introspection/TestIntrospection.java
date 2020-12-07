/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.introspection;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.nuxeo.ecm.core.bulk.introspection.StreamIntrospectionConverter;

/**
 * @since 11.5
 */
public class TestIntrospection {

    @Test
    public void testPumlConvertion() throws Exception {
        String json = readFile("data/introspection.json");
        StreamIntrospectionConverter convert = new StreamIntrospectionConverter(json);
        String puml = convert.getPuml();
        assertTrue(puml.contains("@startuml"));
        assertTrue(puml.contains("@enduml"));
    }

    @Test
    public void testPumlConvertionSimple() throws Exception {
        String json = readFile("data/simple.json");
        StreamIntrospectionConverter convert = new StreamIntrospectionConverter(json);
        String puml = convert.getPuml();
        assertEquals(readFile("data/simple.puml"), puml);
    }

    protected String readFile(String path) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            return IOUtils.toString(is, UTF_8);
        }
    }

}
