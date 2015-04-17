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
	public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
		if (contribution instanceof GwtWarStrategy) {
			GwtWarStrategy descriptor = (GwtWarStrategy) contribution;
			resolver.install(descriptor.name, descriptor.strategy);
		} else if (contribution instanceof GwtWarDirectory) {
			GwtWarDirectory descriptor = (GwtWarDirectory) contribution;
			try {
				resolver.install(descriptor.name, descriptor.dir.toURI());
			} catch (IOException cause) {
				throw new NuxeoException("Cannot install " + descriptor, cause);
			}
		} else if (contribution instanceof GwtWarBundle) {
			GwtWarBundle descriptor = (GwtWarBundle) contribution;
			URL location = contributor.getContext().getBundle().getEntry(descriptor.pathname);
			if (location == null) {
				throw new NuxeoException("Cannot locate GWT " + descriptor + " in "
						+ contributor.getContext().getBundle());
			}
			try {
				resolver.install(descriptor.name, location.toURI());
			} catch (IOException | URISyntaxException cause) {
				throw new NuxeoException("Cannot install " + descriptor, cause);
			}
		}
	}

	@Override
	public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
		if (contribution instanceof GwtWarStrategy) {
			GwtWarStrategy descriptor = (GwtWarStrategy) contribution;
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
