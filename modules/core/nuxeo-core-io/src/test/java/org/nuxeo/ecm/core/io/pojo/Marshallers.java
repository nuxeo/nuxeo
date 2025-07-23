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
package org.nuxeo.ecm.core.io.pojo;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.nuxeo.ecm.core.io.registry.Reader;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.io.registry.reflect.Supports;

/**
 * Class holding {@link org.nuxeo.ecm.core.io.registry.Marshaller} for tests.
 *
 * @since 2025.6
 */
public final class Marshallers {

    private Marshallers() {
        // factory class
    }

    // ------
    // Reader
    // ------

    @Setup(mode = SINGLETON, priority = REFERENCE)
    @Supports(APPLICATION_JSON)
    public static class GrandParentReader implements DefaultReader<GrandParent> {
    }

    @Setup(mode = SINGLETON, priority = REFERENCE)
    @Supports(APPLICATION_JSON)
    public static class GrandParentListReader implements DefaultReader<List<GrandParent>> {
    }

    @Setup(mode = SINGLETON, priority = REFERENCE)
    @Supports(APPLICATION_JSON)
    public static class ParentReader implements DefaultReader<Parent> {
    }

    @Setup(mode = SINGLETON, priority = REFERENCE)
    @Supports(APPLICATION_JSON)
    public static class ParentListReader implements DefaultReader<List<Parent>> {
    }

    @Setup(mode = SINGLETON, priority = REFERENCE)
    @Supports(APPLICATION_JSON)
    public static class ChildReader implements DefaultReader<Child> {
    }

    @Setup(mode = SINGLETON, priority = REFERENCE)
    @Supports(APPLICATION_JSON)
    public static class ChildListReader implements DefaultReader<List<Child>> {
    }

    public interface DefaultReader<EntityType> extends Reader<EntityType> {

        @Override
        default boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
            return true;
        }

        @Override
        default EntityType read(Class<?> clazz, Type genericType, MediaType mediaType, InputStream in)
                throws IOException {
            return null;
        }
    }

    // ------
    // Writer
    // ------

    @Setup(mode = SINGLETON, priority = REFERENCE)
    @Supports(APPLICATION_JSON)
    public static class GrandParentWriter implements DefaultWriter<GrandParent> {
    }

    @Setup(mode = SINGLETON, priority = REFERENCE)
    @Supports(APPLICATION_JSON)
    public static class GrandParentListWriter implements DefaultWriter<List<GrandParent>> {
    }

    @Setup(mode = SINGLETON, priority = REFERENCE)
    @Supports(APPLICATION_JSON)
    public static class ParentWriter implements DefaultWriter<Parent> {
    }

    @Setup(mode = SINGLETON, priority = REFERENCE)
    @Supports(APPLICATION_JSON)
    public static class ParentListWriter implements DefaultWriter<List<Parent>> {
    }

    @Setup(mode = SINGLETON, priority = REFERENCE)
    @Supports(APPLICATION_JSON)
    public static class ChildWriter implements DefaultWriter<Child> {
    }

    @Setup(mode = SINGLETON, priority = REFERENCE)
    @Supports(APPLICATION_JSON)
    public static class ChildListWriter implements DefaultWriter<List<Child>> {
    }

    public interface DefaultWriter<EntityType> extends Writer<EntityType> {

        @Override
        default boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
            return true;
        }

        @Override
        default void write(EntityType entity, Class<?> clazz, Type genericType, MediaType mediatype, OutputStream out)
                throws IOException {
        }
    }
}
