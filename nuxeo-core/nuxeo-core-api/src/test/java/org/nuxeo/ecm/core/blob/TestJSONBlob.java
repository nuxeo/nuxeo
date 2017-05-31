/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.blob;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.impl.blob.JSONBlob;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class TestJSONBlob {

    @Test
    public void testString() throws Exception {
        Blob blob;
        blob = Blobs.createJSONBlob("abc");
        assertEquals("application/json", blob.getMimeType());
        assertEquals("UTF-8", blob.getEncoding());
        assertEquals("abc", blob.getString());
        blob = Blobs.createJSONBlob("\"abc\"");
        assertEquals("\"abc\"", blob.getString());
        blob = Blobs.createJSONBlob("{\"str\":\"abc\"}");
        assertEquals("{\"str\":\"abc\"}", blob.getString());
    }

    @Test
    public void testValueSimple() throws Exception {
        Blob blob;
        blob = Blobs.createJSONBlobFromValue("abc");
        assertEquals("\"abc\"", blob.getString());
        blob = Blobs.createJSONBlobFromValue(Boolean.TRUE);
        assertEquals("true", blob.getString());
        blob = Blobs.createJSONBlobFromValue(Long.valueOf(123456789012L));
        assertEquals("123456789012", blob.getString());
    }

    @Test
    public void testValueArray() throws Exception {
        Object[] array = new Object[] { "abc", Boolean.TRUE, Long.valueOf(123456789012L) };
        Blob blob = Blobs.createJSONBlobFromValue(array);
        assertEquals("[\"abc\",true,123456789012]", blob.getString());
    }

    @Test
    public void testValueList() throws Exception {
        List<Object> list = Arrays.asList("abc", Boolean.TRUE, Long.valueOf(123456789012L));
        Blob blob = Blobs.createJSONBlobFromValue(list);
        assertEquals("[\"abc\",true,123456789012]", blob.getString());
    }

    @Test
    public void testValueMap() throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("str", "abc");
        map.put("bool", Boolean.TRUE);
        map.put("long", Long.valueOf(123456789012L));
        Blob blob = Blobs.createJSONBlobFromValue(map);
        assertEquals("{\"str\":\"abc\",\"bool\":true,\"long\":123456789012}", blob.getString());
    }

    @Test
    public void testValue() throws Exception {
        Object value = new DummyValue("abc");
        Blob blob = Blobs.createJSONBlobFromValue(value);
        assertEquals("{\"str\":\"abc\"}", blob.getString());
    }

    @Test
    public void testValueJackson1() throws Exception {
        Object value = new DummyValueJackson1("abc");
        Blob blob = Blobs.createJSONBlobFromValueJackson1(value);
        assertEquals("{\"str\":\"abc\"}", blob.getString());
    }

    protected static class DummyValue {

        protected final String string;

        public DummyValue(String string) {
            this.string = string;
        }

        /**
         * This is returned as JSON.
         */
        public String getStr() {
            return string;
        }

        /**
         * This is NOT returned as JSON.
         */
        @JsonIgnore
        public String getDebugInfo() {
            return "debuginfo";
        }
    }

    protected static class DummyValueJackson1 {

        protected final String string;

        public DummyValueJackson1(String string) {
            this.string = string;
        }

        /**
         * This is returned as JSON.
         */
        public String getStr() {
            return string;
        }

        /**
         * This is NOT returned as JSON (Jackson 1 annotation).
         */
        @org.codehaus.jackson.annotate.JsonIgnore
        public String getDebugInfo() {
            return "debuginfo";
        }
    }

}
