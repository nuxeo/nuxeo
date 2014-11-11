/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Stephane Lacoin (Nuxeo EP Software Engineer)
 */
package org.nuxeo.ecm.platform.audit.service;

import java.util.Properties;

public class ContainerManagedHibernateConfiguration implements
        HibernateConfiguration {

    String datasource = "jdbc/nxaudits";

    public ContainerManagedHibernateConfiguration() {
    }

    public ContainerManagedHibernateConfiguration(String defaultDatasource) {
        this.datasource = defaultDatasource;
    }

    public Properties getProperties() {
        Properties properties = new Properties();
        properties.put("hibernate.connection.autocommit", true);
        properties.put("hibernate.show_sql", false);
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.connection.datasource", datasource);
        return properties;
    }

}
