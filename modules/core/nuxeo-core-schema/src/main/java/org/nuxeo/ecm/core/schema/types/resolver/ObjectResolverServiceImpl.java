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

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Simple Map base implementation.
 *
 * @since 7.1
 */
public class ObjectResolverServiceImpl extends DefaultComponent implements ObjectResolverService {

    private static final Log log = LogFactory.getLog(ObjectResolverServiceImpl.class);

    private static final String XP = "resolvers";

    @Override
    public ObjectResolver getResolver(String type, Map<String, String> parameters) {
        ObjectResolverDescriptor desc = this.<ObjectResolverDescriptor> getRegistryContribution(XP, type).orElse(null);
        if (desc == null) {
            return null;
        }
        Class<? extends ObjectResolver> resolverClass = desc.getResolver();
        if (resolverClass == null) {
            return null;
        }
        ObjectResolver resolver = null;
        try {
            resolver = resolverClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
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
