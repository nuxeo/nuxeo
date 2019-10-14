/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.server.jaxrs;

import static org.nuxeo.ecm.core.io.APIVersion.API_VERSION_ATTRIBUTE_NAME;

import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.core.Context;

import org.nuxeo.ecm.automation.jaxrs.io.documents.BusinessAdapterListWriter;
import org.nuxeo.ecm.automation.jaxrs.io.operations.MultiPartFormRequestReader;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.io.APIVersion;
import org.nuxeo.ecm.platform.web.common.RequestContext;
import org.nuxeo.ecm.restapi.jaxrs.io.conversion.ConversionScheduledWriter;
import org.nuxeo.ecm.restapi.jaxrs.io.conversion.ConversionStatusWithResultWriter;
import org.nuxeo.ecm.restapi.jaxrs.io.types.DocumentTypesWriter;
import org.nuxeo.ecm.restapi.jaxrs.io.types.FacetsWriter;
import org.nuxeo.ecm.restapi.jaxrs.io.types.SchemasWriter;
import org.nuxeo.ecm.webengine.app.WebEngineModule;
import org.nuxeo.ecm.webengine.jaxrs.coreiodelegate.CoreIODelegate;

import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;

/**
 * @since 5.8
 */
public class APIModule extends WebEngineModule {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> result = super.getClasses();
        // need to be stateless since it needs the request member to be
        // injected
        result.add(MultiPartFormRequestReader.class);
        return result;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> result = new LinkedHashSet<>();

        // REST API Version provider
        result.add(new APIVersionProvider());

        // writers
        result.add(new BusinessAdapterListWriter());
        result.add(new SchemasWriter());
        result.add(new DocumentTypesWriter());
        result.add(new FacetsWriter());
        result.add(new ConversionScheduledWriter());
        result.add(new ConversionStatusWithResultWriter());

        // nuxeo-core-io MarshallerRegistry service reading and writing
        result.add(new CoreIODelegate());

        return result;
    }

    /**
     * Provider to inject the {@link APIVersion} object if present in the request attributes.
     * <p>
     * Throws a {@link NuxeoException} if trying to inject an {@link APIVersion} while there is none in the request
     * attributes.
     *
     * @since 11.1
     */
    public static class APIVersionProvider implements InjectableProvider<Context, Type>, Injectable<APIVersion> {

        @Override
        public APIVersion getValue() {
            return Optional.ofNullable(RequestContext.getActiveContext())
                           .map(RequestContext::getRequest)
                           .map(req -> req.getAttribute(API_VERSION_ATTRIBUTE_NAME))
                           .map(APIVersion.class::cast)
                           .orElseThrow(() -> new NuxeoException("No REST API version found in the request"));
        }

        @Override
        public ComponentScope getScope() {
            return ComponentScope.PerRequest;
        }

        @Override
        public Injectable<APIVersion> getInjectable(ComponentContext ic, Context a, Type c) {
            if (!c.equals(APIVersion.class)) {
                return null;
            }
            return this;
        }

    }
}
