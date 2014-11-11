package org.nuxeo.runtime.test;

import org.nuxeo.runtime.model.impl.AbstractRuntimeContext;
import org.osgi.framework.Bundle;

public class TestRuntimeContext extends AbstractRuntimeContext {

	protected final Bundle bundle;

	public TestRuntimeContext(Bundle bundle) {
	    super(bundle.getSymbolicName());
		this.bundle = bundle;
	}

	@Override
	public Bundle getBundle() {
		return bundle;
	}


}
