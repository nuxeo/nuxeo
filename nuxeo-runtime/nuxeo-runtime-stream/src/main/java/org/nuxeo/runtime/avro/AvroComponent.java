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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.kafka.KafkaConfigServiceImpl;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Avro component.
 *
 * @since 10.2
 */
public class AvroComponent extends DefaultComponent {

    public static final String XP_SCHEMA = "schema";

    public static final String XP_MAPPER = "mapper";

    public static final String XP_FACTORY = "factory";

    public static final String XP_REPLACEMENT = "replacement";

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
    public int getApplicationStartedOrder() {
        return KafkaConfigServiceImpl.APPLICATION_STARTED_ORDER;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void start(ComponentContext context) {
        super.start(context);
        // schema factories can be give to the constructor since they don't need a service instance
        List<AvroSchemaFactoryDescriptor> factoryDescs = getDescriptors(XP_FACTORY);
        Map<Class<?>, Class<AvroSchemaFactory<?>>> factories = new HashMap<>(factoryDescs.size());
        for (AvroSchemaFactoryDescriptor descriptor : factoryDescs) {
            try {
                Class<Object> type = (Class<Object>) Class.forName(descriptor.type);
                factories.put(type, (Class<AvroSchemaFactory<?>>) Class.forName(descriptor.klass));
            } catch (ReflectiveOperationException e) {
                throw new RuntimeServiceException(e);
            }
        }
        // as well as replacements
        List<AvroReplacementDescriptor> replacementDescs = getDescriptors(XP_REPLACEMENT);
        AvroServiceImpl impl = new AvroServiceImpl(replacementDescs, factories);
        // mappers are instantiated with an instance of the service
        List<AvroMapperDescriptor> mapperDescs = getDescriptors(XP_MAPPER);
        Map<Class<?>, AvroMapper<?, ?>> mappers = new HashMap<>(mapperDescs.size());
        for (AvroMapperDescriptor descriptor : mapperDescs) {
            try {
                Class<Object> type = (Class<Object>) Class.forName(descriptor.type);
                Class<AvroMapper<?, ?>> clazz = (Class<AvroMapper<?, ?>>) Class.forName(descriptor.klass);
                Constructor<AvroMapper<?, ?>> constructor = clazz.getConstructor(AvroService.class);
                mappers.put(type, constructor.newInstance(impl));
            } catch (ReflectiveOperationException e) {
                throw new RuntimeServiceException(e);
            }
        }
        // and are added to the service implementation
        impl.setMappers(mappers);
        List<AvroSchemaDescriptor> schemaDescs = getDescriptors(XP_SCHEMA);
        // schemas are registered through the SchemaService interface
        for (AvroSchemaDescriptor descriptor : schemaDescs) {
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
    public void stop(ComponentContext context) throws InterruptedException {
        super.stop(context);
        avroService = null;
    }

}
