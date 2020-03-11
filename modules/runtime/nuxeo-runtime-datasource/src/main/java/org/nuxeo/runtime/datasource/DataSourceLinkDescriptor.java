/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        Framework.getService(PooledDataSourceRegistry.class).createAlias(DataSourceHelper.relativize(name), pool);
    }

    public void unbindSelf(Context namingContext) throws NamingException {
        namingContext.unbind(name);
    }

}
