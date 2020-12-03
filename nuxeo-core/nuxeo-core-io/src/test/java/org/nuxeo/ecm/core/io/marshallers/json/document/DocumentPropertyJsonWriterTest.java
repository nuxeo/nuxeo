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
 *     Thomas Roger
 */
package org.nuxeo.ecm.core.io.marshallers.json.document;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Serializable;

import org.junit.Test;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;

/**
 * since 11.5
 */
public class DocumentPropertyJsonWriterTest extends AbstractJsonWriterTest.Local<DocumentPropertyJsonWriter, Property> {

    public DocumentPropertyJsonWriterTest() {
        super(DocumentPropertyJsonWriter.class, Property.class);
    }

    // NXP-29840
    @Test
    public void testLongValueWithDoubleType() throws IOException {
        Property property = mockProperty(DoubleType.INSTANCE, 123L);
        JsonAssert json = jsonAssert(property);
        json.isDouble();
        json.isEquals(123.0, 0);
    }

    // NXP-29840
    @Test
    public void testIntegerValueWithDoubleType() throws IOException {
        Property property = mockProperty(DoubleType.INSTANCE, 123);
        JsonAssert json = jsonAssert(property);
        json.isDouble();
        json.isEquals(123.0, 0);
    }

    // NXP-29840
    @Test
    public void testDoubleValueWithLongType() throws IOException {
        Property property = mockProperty(LongType.INSTANCE, 123.123);
        JsonAssert json = jsonAssert(property);
        json.isInt();
        json.isEquals(123);
    }

    // NXP-29840
    @Test
    public void testDoubleValueWithIntegerType() throws IOException {
        Property property = mockProperty(IntegerType.INSTANCE, 123.123);
        JsonAssert json = jsonAssert(property);
        json.isInt();
        json.isEquals(123);
    }

    protected Property mockProperty(Type type, Serializable value) {
        Property property = mock(Property.class);
        when(property.isScalar()).thenReturn(true);
        when(property.getType()).thenReturn(type);
        when(property.getXPath()).thenReturn(null);
        when(property.getValue()).thenReturn(value);
        return property;
    }
}
