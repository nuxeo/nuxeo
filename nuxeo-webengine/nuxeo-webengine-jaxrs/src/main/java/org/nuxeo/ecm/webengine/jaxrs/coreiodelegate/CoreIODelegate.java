/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.ecm.webengine.jaxrs.coreiodelegate;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Optional;

import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.io.registry.Reader;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.platform.web.common.RequestContext;

import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;

/**
 * A JAX-RS {@link MessageBodyWriter} that try to delegate the marshalling to all nuxeo-core-io {@link Writer} and
 * {@link Reader}. This singleton is also registering an injection of {@link RenderingContext}
 *
 * @since 11.1
 */
@Provider
@Produces({ APPLICATION_JSON, "text/csv" })
public class CoreIODelegate extends PartialCoreIODelegate
        implements InjectableProvider<Context, Type>, Injectable<RenderingContext> {

    @Override
    protected boolean accept(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public RenderingContext getValue() {
        return Optional.ofNullable(RequestContext.getActiveContext())
                       .map(RequestContext::getRequest)
                       .map(RenderingContextWebUtils::getContext)
                       .orElseThrow(() -> new NuxeoException("No RenderingContext in the request")); // shouldn't happen
    }

    @Override
    public ComponentScope getScope() {
        return ComponentScope.PerRequest;
    }

    @Override
    public Injectable getInjectable(ComponentContext ic, Context a, Type c) {
        if (!c.equals(RenderingContext.class)) {
            return null;
        }
        return this;
    }
}
