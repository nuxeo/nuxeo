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

    protected static class AvroReplacementRegistry extends SimpleContributionRegistry<AvroReplacementDescriptor> {
        @Override
        public String getContributionId(AvroReplacementDescriptor contrib) {
            return contrib.forbidden;
        }

        public Collection<AvroReplacementDescriptor> getDescriptors() {
            return currentContribs.values();
        }
    }

    protected static class SchemaDescriptorRegistry extends SimpleContributionRegistry<AvroSchemaDescriptor> {
        @Override
        public String getContributionId(AvroSchemaDescriptor contrib) {
            return contrib.name;
        }

        public Collection<AvroSchemaDescriptor> getDescriptors() {
            return currentContribs.values();
        }
    }

    protected static class SchemaFactoryRegistry extends SimpleContributionRegistry<AvroSchemaFactoryDescriptor> {
        @Override
        public String getContributionId(AvroSchemaFactoryDescriptor contrib) {
            return contrib.type;
        }

        public Collection<AvroSchemaFactoryDescriptor> getDescriptors() {
            return currentContribs.values();
        }
    }

    public static final String SCHEMA_XP = "schema";

    public static final String FACTORY_XP = "factory";

    public static final String REPLACEMENT_XP = "replacement";

    protected final SchemaDescriptorRegistry schemaDescriptors = new SchemaDescriptorRegistry();

    protected final SchemaFactoryRegistry schemaFactoryDescriptors = new SchemaFactoryRegistry();

    protected final AvroReplacementRegistry replacementDescriptors = new AvroReplacementRegistry();

    protected AvroSchemaFactoryService schemaFactoryService;

    protected AvroSchemaStoreService schemaStoreService;

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(schemaStoreService.getClass())) {
            return (T) schemaStoreService;
        } else if (adapter.isAssignableFrom(schemaFactoryService.getClass())) {
            return (T) schemaFactoryService;
        }
        return null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (SCHEMA_XP.equals(extensionPoint)) {
            schemaDescriptors.addContribution((AvroSchemaDescriptor) contribution);
        } else if (FACTORY_XP.equals(extensionPoint)) {
            schemaFactoryDescriptors.addContribution((AvroSchemaFactoryDescriptor) contribution);
        } else if (REPLACEMENT_XP.equals(extensionPoint)) {
            replacementDescriptors.addContribution((AvroReplacementDescriptor) contribution);
        } else {
            throw new RuntimeServiceException("Unknown extension point: " + extensionPoint);
        }
    }

    @Override
    public void start(ComponentContext context) {
        buildSchemaFactoryService();
        buildSchemaStoreService(context);
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        schemaFactoryService = null;
        schemaStoreService = null;
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (SCHEMA_XP.equals(extensionPoint)) {
            schemaDescriptors.removeContribution((AvroSchemaDescriptor) contribution);
        } else if (FACTORY_XP.equals(extensionPoint)) {
            schemaFactoryDescriptors.removeContribution((AvroSchemaFactoryDescriptor) contribution);
        } else if (REPLACEMENT_XP.equals(extensionPoint)) {
            replacementDescriptors.removeContribution((AvroReplacementDescriptor) contribution);
        } else {
            throw new RuntimeServiceException("Unknown extension point: " + extensionPoint);
        }
    }

    @SuppressWarnings("unchecked")
    protected void buildSchemaFactoryService() {
        Collection<AvroReplacementDescriptor> replacements = replacementDescriptors.getDescriptors();
        Collection<AvroSchemaFactoryDescriptor> descriptors = schemaFactoryDescriptors.getDescriptors();
        AvroSchemaFactoryServiceImpl schemaFactoryService = new AvroSchemaFactoryServiceImpl(replacements);
        AvroSchemaFactoryContext avroContext = schemaFactoryService.createContext();
        Map<Class<?>, Class<AvroSchemaFactory<?>>> factories = new HashMap<>(descriptors.size());
        for (AvroSchemaFactoryDescriptor d : descriptors) {
            try {
                Class<Object> type = (Class<Object>) Class.forName(d.type);
                Class<AvroSchemaFactory<?>> factory = (Class<AvroSchemaFactory<?>>) Class.forName(d.clazz);
                // assert the class is instanciable
                Constructor<AvroSchemaFactory<?>> constructor = factory.getConstructor(AvroSchemaFactoryContext.class);
                constructor.newInstance(avroContext);
                // add it to factories
                factories.put(type, factory);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeServiceException(e);
            }
        }
        schemaFactoryService.setFactories(factories);
        this.schemaFactoryService = schemaFactoryService;
    }

    protected void buildSchemaStoreService(ComponentContext context) {
        schemaStoreService = new AvroSchemaStoreServiceImpl();
        for (AvroSchemaDescriptor descriptor : schemaDescriptors.getDescriptors()) {
            URL url = context.getRuntimeContext().getResource(descriptor.file);
            try (InputStream stream = url == null ? null : url.openStream()) {
                if (stream == null) {
                    throw new RuntimeServiceException("Could not load stream for file " + descriptor.file);
                }
                schemaStoreService.addSchema(new Schema.Parser().parse(stream));
            } catch (IOException e) {
                throw new RuntimeServiceException(e);
            }
        }
    }

}
