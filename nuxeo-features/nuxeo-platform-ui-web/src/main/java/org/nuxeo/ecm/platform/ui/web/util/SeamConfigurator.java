package org.nuxeo.ecm.platform.ui.web.util;

import static org.jboss.seam.ScopeType.APPLICATION;

import java.io.Serializable;

import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.core.Init;

@Name("NuxeoSeamConfigurator")
@Scope(APPLICATION)
@Startup
public class SeamConfigurator implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 178687658975L;

	@In(value="org.jboss.seam.core.init")
	Init init;

	@Create
	public void init()
	{
		init.setJbpmInstalled(false);
	}

}
