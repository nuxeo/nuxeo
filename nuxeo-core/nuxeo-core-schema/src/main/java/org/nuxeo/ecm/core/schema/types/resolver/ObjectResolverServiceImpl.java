/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.schema.types.resolver;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Simple Map base implementation.
 *
 * @since 7.1
 */
public class ObjectResolverServiceImpl extends DefaultComponent implements ObjectResolverService {

    private static final Log log = LogFactory.getLog(ObjectResolverServiceImpl.class);

    private Map<String, Class<? extends ObjectResolver>> resolvers;

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        resolvers = new HashMap<String, Class<? extends ObjectResolver>>();
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("resolvers")) {
            ObjectResolverDescriptor erd = (ObjectResolverDescriptor) contribution;
            resolvers.put(erd.getType(), erd.getResolver());
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("resolvers")) {
            ObjectResolverDescriptor erd = (ObjectResolverDescriptor) contribution;
            resolvers.remove(erd.getType());
        }
    }

    @Override
    public ObjectResolver getResolver(String type, Map<String, String> parameters) {
        Class<? extends ObjectResolver> resolverClass = resolvers.get(type);
        if (resolverClass == null) {
            return null;
        }
        ObjectResolver resolver = null;
        try {
            resolver = resolverClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            log.warn(String.format("Unable to instanciate %s - missing public constructor with no param",
                    resolverClass.getCanonicalName()));
            return null;
        }
        try {
            resolver.configure(parameters);
        } catch (IllegalArgumentException e) {
            log.info(String.format("Unable to configure %s with parameters %s", resolverClass.getCanonicalName(),
                    parameters));
            return null;
        }
        return resolver;
    }

}
