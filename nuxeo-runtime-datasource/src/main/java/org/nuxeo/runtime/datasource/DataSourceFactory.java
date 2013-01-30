/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.runtime.datasource;

import java.lang.management.ManagementFactory;
import java.util.Hashtable;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.nuxeo.runtime.api.Framework;

/**
 * JNDI factory for a DataSource that delegates to an tomcat JDBC pool.
 * <p>
 * An instance of this class is registered in JNDI for each datasource
 * configured by the {@link DataSourceComponent}.
 */
public class DataSourceFactory implements ObjectFactory {

    private static final Log log = LogFactory.getLog(DataSourceFactory.class);

     @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx,
            Hashtable<?, ?> env) throws Exception {

        return Framework.getLocalService(DataSourceRegistry.class).getOrCreateDatasource(obj, name, nameCtx, env);
    }

    protected void registerMBean(Name name, DataSource ds) {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName on = new ObjectName(
                    "org.nuxeo:type=DataSource,class=javax.sql.Datasource,name=\""
                            + name.toString() + "\"");
            ds.preRegister(mbs, on);
        } catch (Exception e) {
            log.error("Cannot publish datasource " + name.toString()
                    + " in platform mbean server", e);
        }
    }

}
