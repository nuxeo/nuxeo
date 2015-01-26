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

import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.junit.Test;
import org.nuxeo.ecm.core.io.registry.Writer;

public class TestWriterMarshalledType {

    @Test
    public void canGetSupportedJavaType() throws Exception {
        MarshallerInspector inspector = new MarshallerInspector(IntegerWriter.class);
        Class<?> type = inspector.getMarshalledType();
        assertNotNull(type);
        assertEquals(Integer.class, type);
    }

    @Test
    public void handleInheritedJavaType() throws Exception {
        MarshallerInspector inspector = new MarshallerInspector(InheritWriter.class);
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
        Type listIntegerMap = TestWriterMarshalledType.class.getDeclaredField("listIntegerMapProperty").getGenericType();
        Type map = TestWriterMarshalledType.class.getDeclaredField("mapProperty").getGenericType();
        MarshallerInspector inspector = new MarshallerInspector(ListIntegerMapWriter.class);
        assertNotNull(inspector.getGenericType());
        assertEquals(listIntegerMap, inspector.getGenericType());
        assertNotEquals(map, inspector.getGenericType());
    }

    @Setup(mode = SINGLETON)
    public static class IntegerWriter implements Writer<Integer> {
        @Override
        public boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
            return true;
        }

        @Override
        public void write(Integer entity, Class<?> clazz, Type genericType, MediaType mediatype, OutputStream out) {
        }
    }

    @Setup(mode = SINGLETON, priority = REFERENCE)
    public static class InheritWriter extends IntegerWriter {
    }

    @Setup(mode = SINGLETON)
    public static class ListIntegerMapWriter implements Writer<Map<String, List<Integer>>> {

        @Override
        public boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
            return true;
        }

        @Override
        public void write(Map<String, List<Integer>> entity, Class<?> clazz, Type genericType, MediaType mediatype,
                OutputStream out) {
        }

    }

}
