/*******************************************************************************
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *******************************************************************************/
package org.nuxeo.runtime.datasource;

import javax.naming.Context;
import javax.naming.LinkRef;
import javax.naming.NamingException;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.datasource.PooledDataSourceRegistry.PooledDataSource;

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


    public void bindSelf(Context namingContext) throws NamingException {
        namingContext.bind(name, new LinkRef(global));
        PooledDataSource pool = DataSourceHelper.getDataSource(global, PooledDataSource.class);
        Framework.getLocalService(PooledDataSourceRegistry.class).createAlias(DataSourceHelper.relativize(name),pool);
    }

    public void unbindSelf(Context namingContext) throws NamingException {
        namingContext.unbind(name);
    }

  }
