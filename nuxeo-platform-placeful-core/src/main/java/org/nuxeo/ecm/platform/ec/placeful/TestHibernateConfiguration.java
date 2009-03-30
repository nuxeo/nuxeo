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
 *    Stephane Lacoin (Nuxeo EP Software Engineer)
 */
package org.nuxeo.ecm.platform.ec.placeful;

import java.util.Properties;

import org.nuxeo.ecm.platform.ec.placeful.service.HibernateConfiguration;

public class TestHibernateConfiguration implements
        HibernateConfiguration {

    public Properties getProperties() {
        Properties properties = new Properties();
        properties.put("hibernate.connection.url",
                "jdbc:hsqldb:mem:.;sql.enforce_strict_size=true");
        properties.put("hibernate.connection.driver_class",
                "org.hsqldb.jdbcDriver");
        properties.put("hibernate.connection.auto_commit", "true");
        properties.put("hibernate.connection.pool_size", "1");
        properties.put("hibernate.dialect",
                "org.hibernate.dialect.HSQLDialect");
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.show_sql", "false"); // true to debug
        properties.put("hibernate.format_sql", "true");

        return properties;
    }

}
