/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob;

import java.util.HashMap;
import java.util.Map;

/**
 * Basic configuration for a blob store.
 *
 * @since 11.1
 */
public abstract class AbstractBlobStoreConfiguration extends PropertyBasedConfiguration {

    public final String namespace;

    public AbstractBlobStoreConfiguration(String systemPropertyPrefix, Map<String, String> properties) {
        super(systemPropertyPrefix, properties);
        namespace = getProperty(BlobProviderDescriptor.NAMESPACE);
    }

    public Map<String, String> propertiesWithNamespace(String ns) {
        Map<String, String> newProperties = new HashMap<>(properties);
        String newNamespace;
        if (namespace == null) {
            newNamespace = ns;
        } else {
            newNamespace = namespace + '/' + ns;
        }
        newProperties.put(BlobProviderDescriptor.NAMESPACE, newNamespace);
        return newProperties;
    }
}
