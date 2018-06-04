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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.avro.Schema;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Avro component.
 *
 * @since 10.2
 */
public class AvroComponent extends DefaultComponent {

    public static final int APPLICATION_START_ORDER = -600;

    protected static class AvroMapperDescriptorRegistry extends SimpleContributionRegistry<AvroMapperDescriptor> {
        @Override
        public String getContributionId(AvroMapperDescriptor contrib) {
            return contrib.type;
        }

        public Collection<AvroMapperDescriptor> getDescriptors() {
            return currentContribs.values();
        }
    }

    protected static class AvroReplacementDescriptorRegistry
            extends SimpleContributionRegistry<AvroReplacementDescriptor> {
        @Override
        public String getContributionId(AvroReplacementDescriptor contrib) {
            return contrib.forbidden;
        }

        public Collection<AvroReplacementDescriptor> getDescriptors() {
            return currentContribs.values();
        }
    }

    protected static class AvroSchemaDescriptorRegistry extends SimpleContributionRegistry<AvroSchemaDescriptor> {
        @Override
        public String getContributionId(AvroSchemaDescriptor contrib) {
            return contrib.name;
        }

        public Collection<AvroSchemaDescriptor> getDescriptors() {
            return currentContribs.values();
        }
    }

    protected static class AvroSchemaFactoryDescriptorRegistry
            extends SimpleContributionRegistry<AvroSchemaFactoryDescriptor> {
        @Override
        public String getContributionId(AvroSchemaFactoryDescriptor contrib) {
            return contrib.type;
        }

        public Collection<AvroSchemaFactoryDescriptor> getDescriptors() {
            return currentContribs.values();
        }
    }

    public static final String SCHEMA_XP = "schema";

    public static final String MAPPER_XP = "mapper";

    public static final String FACTORY_XP = "factory";

    public static final String REPLACEMENT_XP = "replacement";

    protected final AvroMapperDescriptorRegistry avroMapperDescriptors = new AvroMapperDescriptorRegistry();

    protected final AvroSchemaDescriptorRegistry schemaDescriptors = new AvroSchemaDescriptorRegistry();

    protected final AvroSchemaFactoryDescriptorRegistry avroSchemaFactoryDescriptors = new AvroSchemaFactoryDescriptorRegistry();

    protected final AvroReplacementDescriptorRegistry replacementDescriptors = new AvroReplacementDescriptorRegistry();

    protected AvroService avroService;

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(avroService.getClass())) {
            return (T) avroService;
        }
        return null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        switch (extensionPoint) {
        case SCHEMA_XP:
            schemaDescriptors.addContribution((AvroSchemaDescriptor) contribution);
            break;
        case MAPPER_XP:
            avroMapperDescriptors.addContribution((AvroMapperDescriptor) contribution);
            break;
        case FACTORY_XP:
            avroSchemaFactoryDescriptors.addContribution((AvroSchemaFactoryDescriptor) contribution);
            break;
        case REPLACEMENT_XP:
            replacementDescriptors.addContribution((AvroReplacementDescriptor) contribution);
            break;
        default:
            throw new RuntimeServiceException("Unknown extension point: " + extensionPoint);
        }
    }

    @Override
    public int getApplicationStartedOrder() {
        return APPLICATION_START_ORDER;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void start(ComponentContext context) {
        // schema factories can be give to the constructor since they don't need a service instance
        Collection<AvroSchemaFactoryDescriptor> factoryDescriptors = avroSchemaFactoryDescriptors.getDescriptors();
        Map<Class<?>, Class<AvroSchemaFactory<?>>> factories = new HashMap<>(factoryDescriptors.size());
        for (AvroSchemaFactoryDescriptor descriptor : factoryDescriptors) {
            try {
                Class<Object> type = (Class<Object>) Class.forName(descriptor.type);
                factories.put(type, (Class<AvroSchemaFactory<?>>) Class.forName(descriptor.clazz));
            } catch (ReflectiveOperationException e) {
                throw new RuntimeServiceException(e);
            }
        }
        // as well as replacements
        AvroServiceImpl impl = new AvroServiceImpl(replacementDescriptors.getDescriptors(), factories);
        // mappers are instantiated with an instance of the service
        Collection<AvroMapperDescriptor> mapperDescriptors = avroMapperDescriptors.getDescriptors();
        Map<Class<?>, AvroMapper<?, ?>> mappers = new HashMap<>(mapperDescriptors.size());
        for (AvroMapperDescriptor descriptor : mapperDescriptors) {
            try {
                Class<Object> type = (Class<Object>) Class.forName(descriptor.type);
                Class<AvroMapper<?, ?>> clazz = (Class<AvroMapper<?, ?>>) Class.forName(descriptor.clazz);
                Constructor<AvroMapper<?, ?>> constructor = clazz.getConstructor(AvroService.class);
                mappers.put(type, constructor.newInstance(impl));
            } catch (ReflectiveOperationException e) {
                throw new RuntimeServiceException(e);
            }
        }
        // and are added to the service implementation
        impl.setMappers(mappers);
        // schemas are registered through the SchemaService interface
        for (AvroSchemaDescriptor descriptor : schemaDescriptors.getDescriptors()) {
            URL url = context.getRuntimeContext().getResource(descriptor.file);
            try (InputStream stream = url == null ? null : url.openStream()) {
                if (stream == null) {
                    throw new RuntimeServiceException("Could not load stream for file " + descriptor.file);
                }
                impl.addSchema(new Schema.Parser().parse(stream));
            } catch (IOException e) {
                throw new RuntimeServiceException(e);
            }
        }
        avroService = impl;
    }

    @Override
    public void stop(ComponentContext context) {
        avroService = null;
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        switch (extensionPoint) {
        case SCHEMA_XP:
            schemaDescriptors.removeContribution((AvroSchemaDescriptor) contribution);
            break;
        case MAPPER_XP:
            avroMapperDescriptors.removeContribution((AvroMapperDescriptor) contribution);
            break;
        case FACTORY_XP:
            avroSchemaFactoryDescriptors.removeContribution((AvroSchemaFactoryDescriptor) contribution);
            break;
        case REPLACEMENT_XP:
            replacementDescriptors.removeContribution((AvroReplacementDescriptor) contribution);
            break;
        default:
            throw new RuntimeServiceException("Unknown extension point: " + extensionPoint);
        }
    }

}
