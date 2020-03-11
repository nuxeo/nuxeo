/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.app.jersey;

import java.lang.reflect.Type;
import java.util.Set;

import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.app.WebEngineApplication;
import org.nuxeo.ecm.webengine.model.WebContext;

import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;

/**
 * Experimental - Can be used to inject WebContext through {@code @Context} annotation. Do not use it for now.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Provider
public class JerseyApplication extends WebEngineApplication implements InjectableProvider<Context, Type> {

    @Override
    public Set<Object> getSingletons() {
        Set<Object> set = super.getSingletons();
        set.add(this);
        return set;
    }

    @Override
    public ComponentScope getScope() {
        return ComponentScope.PerRequest;
    }

    @Override
    public Injectable<?> getInjectable(ComponentContext cc, Context a, Type t) {
        if (!(t instanceof Class<?>)) {
            return null;
        }
        Class<?> c = (Class<?>) t;
        if (c == WebContext.class) {
            return new Injectable<Object>() {
                @Override
                public Object getValue() {
                    return WebEngine.getActiveContext();
                }
            };
        }
        return null;
    }

}
