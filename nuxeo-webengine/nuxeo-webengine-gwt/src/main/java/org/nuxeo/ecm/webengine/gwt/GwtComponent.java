package org.nuxeo.ecm.webengine.gwt;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

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
            GwtAppLocation descriptor = (GwtAppLocation)contribution;
            try {
                resolver.install(descriptor.name, descriptor.dir.toURI());
            } catch (IOException cause) {
                throw new NuxeoException("Cannot install " + descriptor, cause);
            }
        } else if (contribution instanceof GwtAppBundleLocation) {
            GwtAppBundleLocation descriptor = (GwtAppBundleLocation)contribution;
            URL location = contributor.getContext().getBundle().getEntry(descriptor.pathname);
            try {
                resolver.install(descriptor.name, location.toURI());
            } catch (IOException | URISyntaxException cause) {
                throw new NuxeoException("Cannot install " + descriptor, cause);
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
