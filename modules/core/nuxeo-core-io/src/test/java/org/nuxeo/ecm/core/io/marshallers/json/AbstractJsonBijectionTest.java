/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.io.marshallers.json;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;

import javax.inject.Inject;

import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.io.CoreIOFeature;
import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.Reader;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Base class to test bijection of a reader/writer couple
 *
 * @param <MarshalledType> The marshalled type to test.
 * @since 10.2
 */
@RunWith(FeaturesRunner.class)
@Features(CoreIOFeature.class)
public abstract class AbstractJsonBijectionTest<MarshalledType> {

    @Inject
    protected MarshallerRegistry registry;

    private Class<MarshalledType> marshalledClass;

    private Type marshalledGenericType;

    public AbstractJsonBijectionTest(Class<MarshalledType> marshalledClass) {
        this(marshalledClass, marshalledClass);
    }

    public AbstractJsonBijectionTest(Class<MarshalledType> marshalledClass, Type marshalledGenericType) {
        super();
        this.marshalledClass = marshalledClass;
        this.marshalledGenericType = marshalledGenericType;
    }

    public MarshalledType asObject(File file) throws IOException {
        Reader<MarshalledType> reader = registry.getReader(CtxBuilder.get(), marshalledClass, marshalledGenericType,
                APPLICATION_JSON_TYPE);
        try (FileInputStream inputStream = new FileInputStream(file)) {
            return reader.read(marshalledClass, marshalledGenericType, APPLICATION_JSON_TYPE, inputStream);
        }
    }

    public String asJson(MarshalledType object) throws IOException {
        Writer<MarshalledType> writer = registry.getWriter(CtxBuilder.builder().get(), marshalledClass,
                marshalledGenericType, APPLICATION_JSON_TYPE);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            writer.write(object, marshalledClass, marshalledGenericType, APPLICATION_JSON_TYPE, baos);
            return baos.toString();
        }
    }

    public void assertContentEquals(File expectedFile, String actualJson) throws IOException {
        String expectedJson = new String(Files.readAllBytes(expectedFile.toPath()));
        assertEquals(JsonAssert.on(expectedJson).getNode(), JsonAssert.on(actualJson).getNode());
    }

}
