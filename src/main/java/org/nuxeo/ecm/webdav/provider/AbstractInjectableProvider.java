package org.nuxeo.ecm.webdav.provider;

import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.server.impl.inject.AbstractHttpContextInjectable;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;

import javax.ws.rs.core.Context;
import java.lang.reflect.Type;

/**
 * See: http://codahale.com/what-makes-jersey-interesting-injection-providers/
 */
public abstract class AbstractInjectableProvider<E>
      extends AbstractHttpContextInjectable<E>
      implements InjectableProvider<Context, Type> {

    private final Type t;

    protected AbstractInjectableProvider(Type t) {
        this.t = t;
    }

    @Override
    public Injectable<E> getInjectable(ComponentContext ic, Context a, Type c) {
        if (c.equals(t)) {
            return getInjectable(ic, a);
        }
        return null;
    }

    public Injectable<E> getInjectable(ComponentContext ic, Context a) {
        return this;
    }

    @Override
    public ComponentScope getScope() {
        return ComponentScope.PerRequest;
    }

}