/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.ecm.platform.test;

import javax.sql.DataSource;

import org.hsqldb.jdbc.jdbcDataSource;
import org.junit.runners.model.InitializationError;
import org.nuxeo.ecm.core.test.NuxeoCoreRunner;

public class NuxeoPlatformRunner extends NuxeoCoreRunner {

    public NuxeoPlatformRunner(Class<?> classToRun)
            throws InitializationError {
        super(classToRun);
    }

    @Override
    protected void deploy() throws Exception {        
        scanDeployments(PlatformDeployment.class);
    }
    
    @Override
    protected void initialize() throws Exception {
        super.initialize();
        bindDatasource("nxsqldirectory", createDataSource("jdbc:hsqldb:mem:directories"));
    }
    
    public static DataSource createDataSource(String dbName) {
        jdbcDataSource datasource = new jdbcDataSource();
        datasource.setDatabase(dbName);
        datasource.setUser("sa");
        datasource.setPassword("");
        return datasource;
    }
        
}
