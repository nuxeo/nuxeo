/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.schema.types.reference;

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
public class ExternalReferenceServiceImpl extends DefaultComponent implements ExternalReferenceService {

    private static final Log log = LogFactory.getLog(ExternalReferenceServiceImpl.class);

    private Map<String, Class<? extends ExternalReferenceResolver>> resolvers;

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        resolvers = new HashMap<String, Class<? extends ExternalReferenceResolver>>();
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("resolvers")) {
            ExternalReferenceResolverDescriptor erd = (ExternalReferenceResolverDescriptor) contribution;
            resolvers.put(erd.getType(), erd.getResolver());
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("resolvers")) {
            ExternalReferenceResolverDescriptor erd = (ExternalReferenceResolverDescriptor) contribution;
            resolvers.remove(erd.getType());
        }
    }

    @Override
    public ExternalReferenceResolver getResolver(String type, Map<String, String> parameters) {
        Class<? extends ExternalReferenceResolver> resolverClass = resolvers.get(type);
        if (resolverClass == null) {
            return null;
        }
        ExternalReferenceResolver resolver = null;
        try {
            resolver = resolverClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            log.error(
                    String.format("Unable to instanciate %s - missing public constructor with no param",
                            resolverClass.getCanonicalName()), e);
            return null;
        }
        try {
            resolver.configure(parameters != null ? parameters : new HashMap<String, String>());
        } catch (IllegalArgumentException e) {
            log.error(String.format("Unable to configure %s with parameters %s", resolverClass.getCanonicalName(),
                    parameters.toString()), e);
            return null;
        }
        return resolver;
    }

}
