/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.registry.reflect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.junit.Test;
import org.nuxeo.ecm.core.io.registry.Reader;

public class TestReaderMarshalledType {

    @Test
    public void canGetSupportedJavaType() throws Exception {
        MarshallerInspector inspector = new MarshallerInspector(IntegerReader.class);
        Class<?> type = inspector.getMarshalledType();
        assertNotNull(type);
        assertEquals(Integer.class, type);
    }

    @Test
    public void handleInheritedJavaType() throws Exception {
        MarshallerInspector inspector = new MarshallerInspector(InheritReader.class);
        Class<?> type = inspector.getMarshalledType();
        assertNotNull(type);
        assertEquals(Integer.class, type);
    }

    // used for reflect in following test
    @SuppressWarnings("unused")
    private Map<String, List<Integer>> listIntegerMapProperty = null;

    // used for reflect in following test
    @SuppressWarnings("unused")
    private Map<?, ?> mapProperty = null;

    @Test
    public void canGetStereotypedMarshaller() throws Exception {
        Type listIntegerMap = TestReaderMarshalledType.class.getDeclaredField("listIntegerMapProperty")
                                                            .getGenericType();
        Type map = TestReaderMarshalledType.class.getDeclaredField("mapProperty").getGenericType();
        MarshallerInspector inspector = new MarshallerInspector(ListIntegerMapReader.class);
        assertNotNull(inspector.getGenericType());
        assertEquals(listIntegerMap, inspector.getGenericType());
        assertNotEquals(map, inspector.getGenericType());
    }

    @Setup(mode = SINGLETON)
    public static class IntegerReader implements Reader<Integer> {
        @Override
        public boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
            return true;
        }

        @Override
        public Integer read(Class<?> clazz, Type genericType, MediaType mediatype, InputStream in) {
            return null;
        }
    }

    @Setup(mode = SINGLETON, priority = REFERENCE)
    public static class InheritReader extends IntegerReader {
    }

    @Setup(mode = SINGLETON)
    public static class ListIntegerMapReader implements Reader<Map<String, List<Integer>>> {

        @Override
        public boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
            return true;
        }

        @Override
        public Map<String, List<Integer>> read(Class<?> clazz, Type genericType, MediaType mediatype, InputStream in) {
            return null;
        }

    }

}
