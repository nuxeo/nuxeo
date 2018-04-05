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
import java.net.URL;

import org.apache.avro.Schema;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Avro component.
 *
 * @since 10.2
 */
public class AvroComponent extends DefaultComponent {

    public static final String SCHEMA_XP = "schema";

    private static final Log log = LogFactory.getLog(AvroComponent.class);

    protected final AvroSchemaStoreService schemaStoreService;

    public AvroComponent() {
        schemaStoreService = new AvroSchemaStoreServiceImpl();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(schemaStoreService.getClass())) {
            return (T) schemaStoreService;
        }
        return null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(SCHEMA_XP)) {
            AvroSchemaDescriptor descriptor = (AvroSchemaDescriptor) contribution;
            try {
                URL url = contributor.getRuntimeContext().getResource(descriptor.file);
                InputStream stream = url == null ? null : url.openStream();
                if (stream == null) {
                    log.error(String.format("Could not load stream for file %s.", descriptor.file));
                    return;
                }
                schemaStoreService.addSchema(new Schema.Parser().parse(stream));
            } catch (IOException e) {
                log.error(String.format("Could not load stream for file %s. Error message: %s",
                        descriptor.file, e.getMessage()));
            }
        } else {
            throw new RuntimeServiceException("Unknown extension point: " + extensionPoint);
        }
    }

}
