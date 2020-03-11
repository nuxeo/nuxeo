/*
 * (C) Copyright 2015-2019 Nuxeo (http://nuxeo.com/) and others.
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
 * {@link Reader}.
 *
 * @since 7.2
 * @implNote since 11.1, this singleton is also registering an injection of {@link RenderingContext}
 * @deprecated since 11.1. Use {@link org.nuxeo.ecm.webengine.jaxrs.coreiodelegate.CoreIODelegate} instead.
 */
@Deprecated(since = "11.1", forRemoval = true)
@Provider
@Produces(APPLICATION_JSON)
public final class JsonCoreIODelegate extends PartialCoreIODelegate
        implements InjectableProvider<Context, Type>, Injectable<RenderingContext> {

    @Override
    protected boolean accept(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    /**
     * @since 11.1
     */
    @Override
    public RenderingContext getValue() {
        return Optional.ofNullable(RequestContext.getActiveContext())
                       .map(RequestContext::getRequest)
                       .map(RenderingContextWebUtils::getContext)
                       .orElseThrow(() -> new NuxeoException("No RenderingContext in the request")); // shouldn't happen
    }

    /**
     * @since 11.1
     */
    @Override
    public ComponentScope getScope() {
        return ComponentScope.PerRequest;
    }

    /**
     * @since 11.1
     */
    @Override
    public Injectable getInjectable(ComponentContext ic, Context a, Type c) {
        if (!c.equals(RenderingContext.class)) {
            return null;
        }
        return this;
    }
}
