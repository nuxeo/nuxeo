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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.message.SchemaStore;
import org.nuxeo.runtime.RuntimeServiceException;

/**
 * @since 10.2
 */
public class AvroServiceImpl extends SchemaStore.Cache implements AvroService {

    private static final AvroMapper<Object, Object> NULL = new AvroMapper<Object, Object>(null) {

        @Override
        public Object fromAvro(Schema schema, Object input) {
            return null;
        }

        @Override
        public GenericRecord toAvro(Schema schema, Object input) {
            return null;
        }

    };

    protected final Map<Class<?>, Class<AvroSchemaFactory<?>>> factories;

    protected final List<AvroReplacementDescriptor> replacements;

    protected Map<Class<?>, AvroMapper<?, ?>> mappers;

    public AvroServiceImpl(Collection<AvroReplacementDescriptor> replacements,
            Map<Class<?>, Class<AvroSchemaFactory<?>>> factories) {
        this.replacements = new ArrayList<>(replacements);
        this.factories = new HashMap<>(factories);
        Collections.sort(this.replacements);
        // assert at creation that factories are valid
        createContext();
    }

    @Override
    public <D> Schema createSchema(D input) {
        return createContext().createSchema(input);
    }

    @Override
    public String decodeName(String input) {
        String output = input;
        ListIterator<AvroReplacementDescriptor> it = replacements.listIterator(replacements.size());
        while (it.hasPrevious()) {
            AvroReplacementDescriptor descriptor = it.previous();
            output = output.replaceAll(descriptor.replacement, descriptor.forbidden);
        }
        return output;
    }

    @Override
    public String encodeName(String input) {
        String output = input;
        for (AvroReplacementDescriptor descriptor : replacements) {
            output = output.replaceAll(descriptor.forbidden, descriptor.replacement);
        }
        return output;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D, M> D fromAvro(Schema schema, Class<D> clazz, M input) {
        return (D) getMapper(clazz).fromAvro(schema, input);
    }

    public void setMappers(Map<Class<?>, AvroMapper<?, ?>> mappers) {
        this.mappers = new HashMap<>(mappers);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D, M> M toAvro(Schema schema, D input) {
        return (M) getMapper((Class<D>) input.getClass()).toAvro(schema, input);
    }

    protected AvroSchemaFactoryContext createContext() {
        AvroSchemaFactoryContext context = new AvroSchemaFactoryContext(this);
        for (Entry<Class<?>, Class<AvroSchemaFactory<?>>> entry : factories.entrySet()) {
            try {
                Class<AvroSchemaFactory<?>> clazz = entry.getValue();
                Constructor<AvroSchemaFactory<?>> constructor = clazz.getConstructor(AvroSchemaFactoryContext.class);
                AvroSchemaFactory<?> factory = constructor.newInstance(context);
                context.register(entry.getKey(), factory);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeServiceException(e);
            }
        }
        return context;
    }

    @SuppressWarnings("unchecked")
    protected <D, M> AvroMapper<D, M> getMapper(Class<D> clazz) {
        AvroMapper<?, ?> factory = mappers.get(clazz);
        if (factory != null) {
            return (AvroMapper<D, M>) factory;
        }
        for (Class<?> intrface : clazz.getInterfaces()) {
            factory = mappers.get(intrface);
            if (factory != null) {
                return (AvroMapper<D, M>) factory;
            }
        }
        for (Entry<Class<?>, AvroMapper<?, ?>> entry : mappers.entrySet()) {
            if (entry.getKey().isAssignableFrom(clazz)) {
                return (AvroMapper<D, M>) entry.getValue();
            }
        }
        return (AvroMapper<D, M>) NULL;
    }

}
