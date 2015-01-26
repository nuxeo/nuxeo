/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
        Type listIntegerMap = TestReaderMarshalledType.class.getDeclaredField("listIntegerMapProperty").getGenericType();
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
