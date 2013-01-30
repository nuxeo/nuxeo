/*******************************************************************************
 * Copyright (c) 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *******************************************************************************/
package org.nuxeo.runtime.datasource;

import javax.naming.InitialContext;
import javax.naming.LinkRef;
import javax.naming.NamingException;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.DataSourceHelper;

@XObject("link")
public class DataSourceLinkDescriptor {

	protected String name;

	@XNode("@name")
	public void setName(String value) {
	    name = DataSourceHelper.getDataSourceJNDIName(value);
	}

	protected String global;


    @XNode("@global")
    public void setGlobal(String value) {
        global = DataSourceHelper.getDataSourceJNDIName(value);
    }

	@XNode("@type")
	protected String type;

    public void bindSelf(InitialContext namingContext) throws NamingException {
        namingContext.bind(name, new LinkRef(global));
    }
}
