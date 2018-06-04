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
 *     pierre
 */
package org.nuxeo.runtime.avro;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.avro.Schema;

/**
 * An AvroSchemaFactoryContext represents a context in which Avro schemas are cached and reused depending on their
 * qualified name.<br>
 * <br>
 * Avro does not permit to declare twice a schema with the same qualified name. Thus a schema has to be fully described
 * the first time it appears in the object, and then be referred by name.<br>
 *
 * @since 10.2
 */
public class AvroSchemaFactoryContext {

    protected static final AvroSchemaFactory<Object> NULL = new AvroSchemaFactory<Object>(null) {

        @Override
        public Schema createSchema(Object input) {
            return null;
        }

        @Override
        public String getName(Object input) {
            return null;
        }
    };

    protected final Map<Class<?>, AvroSchemaFactory<?>> factories = new HashMap<>();

    protected final Map<String, Schema> createdSchemas = new HashMap<>();

    protected final AvroService service;

    protected AvroSchemaFactoryContext(AvroService service) {
        super();
        this.service = service;
    }

    public <T> Schema createSchema(T input) {
        String qualifiedName = getFactory(input).getQualifiedName(input);
        return createdSchemas.computeIfAbsent(qualifiedName, k -> getFactory(input).createSchema(input));
    }

    public AvroService getService() {
        return service;
    }

    public <U> List<U> sort(Collection<U> children) {
        if (children == null || children.isEmpty()) {
            return Collections.emptyList();
        }
        AvroSchemaFactory<U> factory = getFactory(children.iterator().next());
        if (factory == null) {
            return Collections.emptyList();
        }
        List<U> sortedChildren = new ArrayList<>(children);
        sortedChildren.sort((o1, o2) -> Objects.compare(
                factory.getQualifiedName(o1),
                factory.getQualifiedName(o2),
                String::compareTo));
        return sortedChildren;
    }

    @SuppressWarnings("unchecked")
    protected <T> AvroSchemaFactory<T> getFactory(T input) {
        AvroSchemaFactory<T> factory = (AvroSchemaFactory<T>) factories.get(input.getClass());
        if (factory != null) {
            return factory;
        }
        for (Class<?> intrface : input.getClass().getInterfaces()) {
            factory = (AvroSchemaFactory<T>) factories.get(intrface);
            if (factory != null) {
                return factory;
            }
        }
        for (Entry<Class<?>, AvroSchemaFactory<?>> entry : factories.entrySet()) {
            if (entry.getKey().isAssignableFrom(input.getClass())) {
                return (AvroSchemaFactory<T>) entry.getValue();
            }
        }
        return (AvroSchemaFactory<T>) NULL;
    }

    protected void register(Class<?> type, AvroSchemaFactory<?> factory) {
        factories.put(type, factory);
    }

}
