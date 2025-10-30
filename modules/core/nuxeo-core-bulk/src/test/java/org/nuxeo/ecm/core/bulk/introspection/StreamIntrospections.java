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
package org.nuxeo.ecm.core.bulk.introspection;

import static org.nuxeo.common.test.ModuleUnderTest.getClassLoaderResourceAsString;

import java.io.IOException;

import org.nuxeo.ecm.core.io.registry.MarshallerHelper;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;

/**
 * @since 2025.12
 */
final class StreamIntrospections {

    private StreamIntrospections() {
        // factory class
    }

    public static StreamIntrospection getClassLoaderResourceAsStreamIntrospection(String name) throws IOException {
        String json = getClassLoaderResourceAsString(name);
        return getJsonAsStreamIntrospection(json);
    }

    public static StreamIntrospection getJsonAsStreamIntrospection(String json) throws IOException {
        return MarshallerHelper.jsonToObject(StreamIntrospection.class, json, RenderingContext.CtxBuilder.get());
    }
}
