package org.nuxeo.ecm.webengine.gwt;

import java.io.IOException;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class GwtComponent extends DefaultComponent {


    protected final GwtResolver resolver = new GwtResolver();

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor)  {
        if (contribution instanceof GwtAppResolver) {
            GwtAppResolver descriptor = (GwtAppResolver) contribution;
            resolver.install(descriptor.name, descriptor.strategy);
        } else if (contribution instanceof GwtAppLocation) {
            GwtAppLocation location = (GwtAppLocation)contribution;
            try {
                resolver.install(location.name, location.dir.toURI());
            } catch (IOException cause) {
                throw new NuxeoException("Cannot install " + location, cause);
            }
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (contribution instanceof GwtAppResolver) {
            GwtAppResolver descriptor = (GwtAppResolver) contribution;
            resolver.uninstall(descriptor.name);
        }
    }


    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(GwtResolver.class)) {
            return adapter.cast(resolver);
        }
        return super.getAdapter(adapter);
    }

}
