/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.theme.webwidgets.providers;

import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ejb.Ejb3Configuration;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

public class PersistenceConfigurator extends DefaultComponent implements
        FrameworkListener {

    private static final Log log = LogFactory.getLog(PersistenceConfigurator.class);

    protected static EntityManagerFactory emf;

    protected static DataSource ds;

    private static final String DEFAULT_DATASOURCE = "jdbc/nxwebwidgets";

    @Override
    public void activate(ComponentContext context) throws Exception {
        context.getRuntimeContext().getBundle().getBundleContext().addFrameworkListener(
                this);
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        // this is doing nothing if listener was not registered
        context.getRuntimeContext().getBundle().getBundleContext().removeFrameworkListener(
                this);
    }

    public void frameworkEvent(FrameworkEvent event) {
        if (event.getType() == FrameworkEvent.STARTED) {
            ClassLoader jbossCL = Thread.currentThread().getContextClassLoader();
            ClassLoader nuxeoCL = PersistenceConfigurator.class.getClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(nuxeoCL);
                log.info("Service initialization");
                initPersistenceUnit();
            } finally {
                Thread.currentThread().setContextClassLoader(jbossCL);
                log.debug("Server ClassLoader restored");
            }
        }
    }

    public Properties getProperties() {
        Properties properties = new Properties();
        properties.put("hibernate.connection.autocommit", true);
        properties.put("hibernate.show_sql", false);
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.connection.datasource", DEFAULT_DATASOURCE);
        return properties;
    }

    protected synchronized void initPersistenceUnit() {
        Ejb3Configuration cfg = new Ejb3Configuration();
        cfg.configure("fake-hibernate.cfg.xml");
        cfg.addProperties(getProperties());
        cfg.addAnnotatedClass(WidgetEntity.class);
        cfg.addAnnotatedClass(DataEntity.class);
        emf = cfg.buildEntityManagerFactory();
    }

    public static EntityManager getEntityManager() {
        if (emf != null) {
            return emf.createEntityManager();
        } else {
            log.error("Unable to get EntityManager, there is no factory");
            return null;
        }
    }

}
