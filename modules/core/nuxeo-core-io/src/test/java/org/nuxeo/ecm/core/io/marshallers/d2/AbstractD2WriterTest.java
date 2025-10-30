/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.core.io.marshallers.d2;

import static org.nuxeo.ecm.core.io.marshallers.NuxeoMediaType.TEXT_D2_TYPE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.nuxeo.ecm.core.io.marshallers.AbstractWriterTest;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * @since 2025.12
 */
public abstract class AbstractD2WriterTest<WriterClass extends Writer<MarshalledType>, MarshalledType>
        extends AbstractWriterTest<WriterClass, MarshalledType> {

    public static abstract class Local<WriterClass extends Writer<MarshalledType>, MarshalledType>
            extends AbstractD2WriterTest<WriterClass, MarshalledType> {
    }

    @Deploy("org.nuxeo.runtime.stream")
    public static abstract class External<WriterClass extends Writer<MarshalledType>, MarshalledType>
            extends AbstractD2WriterTest<WriterClass, MarshalledType> {
    }

    public String asD2(MarshalledType object) throws IOException {
        return asD2(object, RenderingContext.CtxBuilder.get());
    }

    public String asD2(MarshalledType object, RenderingContext ctx) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        getInstance(ctx).write(object, marshalledClass, marshalledGenericType, TEXT_D2_TYPE, baos);
        return baos.toString();
    }
}
