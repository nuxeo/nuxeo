package org.nuxeo.ecm.platform.audit.service.extension;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("adapter")
public class AdapterDescriptor {

	@XNode("@name")
	private String name;

	@XNode("@class")
	protected Class<?> klass;

	public String getName() {
		return name;
	}
	
	public Class<?> getKlass() {
		return klass;
	}

}
