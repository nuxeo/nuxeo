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
package org.nuxeo.ecm.core.io.marshallers;

import java.lang.reflect.Type;

import org.nuxeo.ecm.core.io.registry.Reader;

/**
 * @since 2025.12
 */
public abstract class AbstractReaderTest<ReaderClass extends Reader<MarshalledType>, MarshalledType>
        extends AbstractMarshallerTest<ReaderClass, MarshalledType> {

    protected final Class<ReaderClass> readerClass;

    public AbstractReaderTest() {
        super();
        this.readerClass = super.marshallerClass;
    }

    /** @deprecated since 2025.12, use {@link #AbstractReaderTest()} instead */
    @SuppressWarnings("removal") // weirdly detected as still used
    @Deprecated(since = "2025.12", forRemoval = true)
    protected AbstractReaderTest(Class<ReaderClass> readerClass, Class<?> marshalledClass, Type marshalledGenericType) {
        super(readerClass, marshalledClass, marshalledGenericType);
        this.readerClass = readerClass;
    }
}
