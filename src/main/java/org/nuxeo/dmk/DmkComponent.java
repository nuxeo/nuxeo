package org.nuxeo.dmk;

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class DmkComponent extends DefaultComponent {
	
	protected DmkConfig config;
	
	@Override
	public void activate(ComponentContext context) throws Exception {
		super.activate(context);
	}
	
	@Override
	public void registerContribution(Object contribution,
			String extensionPoint, ComponentInstance contributor)
			throws Exception {
		if ("config".equals(extensionPoint)) {
			config = (DmkConfig)contribution;
		} else {
			super.registerContribution(contribution, extensionPoint, contributor);
		}
	}

}
