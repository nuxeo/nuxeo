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

import javax.inject.Inject;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.ReflectUtils;
import org.nuxeo.ecm.core.io.CoreIOFeature;
import org.nuxeo.ecm.core.io.registry.Marshaller;
import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 2025.12
 */
@RunWith(FeaturesRunner.class)
@Features(CoreIOFeature.class)
abstract class AbstractMarshallerTest<MarshallerClass extends Marshaller<MarshalledType>, MarshalledType> {

    protected final Class<MarshallerClass> marshallerClass;

    protected final Class<MarshalledType> marshalledClass;

    protected final Type marshalledGenericType;

    @Inject
    protected MarshallerRegistry registry;

    @SuppressWarnings("unchecked")
    public AbstractMarshallerTest() {
        var types = ReflectUtils.retrieveParameterTypes(getClass(), AbstractMarshallerTest.class, "MarshallerClass",
                "MarshalledType");
        this.marshallerClass = (Class<MarshallerClass>) types[0];
        this.marshalledGenericType = types[1];
        this.marshalledClass = (Class<MarshalledType>) TypeUtils.getRawType(this.marshalledGenericType, null);
    }

    /** @deprecated since 2025.12, use {@link #AbstractMarshallerTest()} instead */
    @SuppressWarnings("unchecked")
    @Deprecated(since = "2025.12", forRemoval = true)
    protected AbstractMarshallerTest(Class<MarshallerClass> marshallerClass, Class<?> marshalledClass,
            Type marshalledGenericType) {
        super();
        this.marshallerClass = marshallerClass;
        this.marshalledClass = (Class<MarshalledType>) marshalledClass;
        this.marshalledGenericType = marshalledGenericType;
    }

    public MarshallerClass getInstance() {
        return registry.getInstance(RenderingContext.CtxBuilder.get(), marshallerClass);
    }

    public MarshallerClass getInstance(RenderingContext ctx) {
        return registry.getInstance(ctx, marshallerClass);
    }
}
