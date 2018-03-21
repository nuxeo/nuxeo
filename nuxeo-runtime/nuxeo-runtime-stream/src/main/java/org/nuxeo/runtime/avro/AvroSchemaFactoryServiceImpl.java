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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import org.nuxeo.runtime.RuntimeServiceException;

/**
 * @since 10.2
 */
public class AvroSchemaFactoryServiceImpl implements AvroSchemaFactoryService {

    protected final SortedSet<AvroReplacementDescriptor> replacements = new TreeSet<>();

    protected Map<Class<?>, Class<AvroSchemaFactory<?>>> factories = new HashMap<>();

    public AvroSchemaFactoryServiceImpl(Collection<AvroReplacementDescriptor> replacements) {
        for (AvroReplacementDescriptor replacement : replacements) {
            this.replacements.add(replacement);
        }
    }

    public AvroSchemaFactoryServiceImpl() {
        this(Collections.emptyList());
    }

    public void setFactories(Map<Class<?>, Class<AvroSchemaFactory<?>>> factories) {
        this.factories.clear();
        this.factories.putAll(factories);
    }

    @Override
    public AvroSchemaFactoryContext createContext() {
        AvroSchemaFactoryContext context = new AvroSchemaFactoryContext(replacements);
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

}
