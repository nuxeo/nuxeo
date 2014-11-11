/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.app.jersey;

import java.lang.reflect.Type;
import java.util.Set;

import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.app.WebEngineApplication;
import org.nuxeo.ecm.webengine.model.WebContext;

import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;

/**
 * Experimental - Can be used to inject WebContext through {@code @Context} annotation.
 * Do not use it for now.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Provider
public class JerseyApplication extends WebEngineApplication implements InjectableProvider<Context, Type> {

    private static final Log log = LogFactory.getLog(JerseyApplication.class);

    @Override
    public Set<Object> getSingletons() {
        Set<Object> set = super.getSingletons();
        set.add(this);
        return set;
    }

    public ComponentScope getScope() {
        return ComponentScope.PerRequest;
    }

    public Injectable<?> getInjectable(ComponentContext cc, Context a, Type t) {
        if (!(t instanceof Class<?>)) {
            return null;
        }

        try {
            Class<?> c = (Class<?>)t;
            if (c == WebContext.class) {
                return new Injectable<Object>() {
                    public Object getValue() {
                        return WebEngine.getActiveContext();
                    }
                };
            }
            return null;
        } catch (Exception e) {
            log.error(e, e);
            return null;
        }
    }

}
