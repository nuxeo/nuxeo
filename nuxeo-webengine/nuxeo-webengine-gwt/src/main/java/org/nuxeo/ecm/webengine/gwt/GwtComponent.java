package org.nuxeo.ecm.webengine.gwt;

import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class GwtComponent extends DefaultComponent {


    protected final GwtResolver resolver = new GwtResolver();

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (contribution instanceof GwtAppDescriptor) {
            GwtAppDescriptor descriptor = (GwtAppDescriptor) contribution;
            resolver.install(descriptor.name, descriptor.resolver);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (contribution instanceof GwtAppDescriptor) {
            GwtAppDescriptor descriptor = (GwtAppDescriptor) contribution;
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
