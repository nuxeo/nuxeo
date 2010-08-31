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
 *     "Stephane Lacoin (aka matic) <slacoin@nuxeo.org>"
 */
package org.nuxeo.ecm.core.persistence;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.ejb.HibernatePersistence;
import org.hibernate.ejb.transaction.JoinableCMTTransactionFactory;
import org.hibernate.transaction.TransactionManagerLookup;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.DataSourceHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @author "Stephane Lacoin (aka matic) <slacoin@nuxeo.org>"
 */
@XObject("hibernateConfiguration")
public class HibernateConfiguration implements EntityManagerFactoryProvider {

    public static final String RESOURCE_LOCAL = PersistenceUnitTransactionType.RESOURCE_LOCAL.name();

    public static final String JTA = PersistenceUnitTransactionType.JTA.name();

    public static final String TXTYPE_PROPERTY_NAME = "org.nuxeo.runtime.txType";

    private static final Log log = LogFactory.getLog(HibernateConfiguration.class);

    @XNode("@name")
    public String name;

    @XNode("datasource")
    public void setDatasource(String name) {
        String expandedValue = Framework.expandVars(name);
        if (expandedValue.startsWith("$")) {
            throw new PersistenceError("Cannot expand " + name
                    + " for datasource");
        }
        hibernateProperties.put("hibernate.connection.datasource",
                DataSourceHelper.getDataSourceJNDIName(name));
    }

    @XNodeMap(value = "properties/property", key = "@name", type = Properties.class, componentType = String.class)
    public final Properties hibernateProperties = new Properties();

    @XNodeList(value = "classes/class", type = ArrayList.class, componentType = Class.class)
    public final List<Class<?>> annotedClasses = new ArrayList<Class<?>>();

    public void addAnnotedClass(Class<?> annotedClass) {
        annotedClasses.add(annotedClass);
    }

    public void removeAnnotedClass(Class<?> annotedClass) {
        annotedClasses.remove(annotedClass);
    }

    protected Ejb3Configuration cfg;

    public Ejb3Configuration setupConfiguration() {
        cfg = new Ejb3Configuration();

        cfg.configure(name, Collections.emptyMap());

        // Load hibernate properties
        cfg.setProperties(hibernateProperties);

        // Add annnoted classes if any
        for (Class<?> annotedClass : annotedClasses) {
            cfg.addAnnotatedClass(annotedClass);
        }

        // needed for correct setup
        cfg.configure("fake-hibernate.cfg.xml");

        return cfg;
    }

    public EntityManagerFactory getFactory(String txType) {
        if (cfg == null) {
            setupConfiguration();
        }
        Map<String, String> properties = new HashMap<String, String>();
        if (txType == null) {
            txType = getTxType();
        }
        if (txType != null) {
            properties.put(HibernatePersistence.TRANSACTION_TYPE, txType);
            if (txType.equals(JTA)) {
                // properties.put(Environment.TRANSACTION_STRATEGY,
                // JTATransactionFactory.class.getName());
                properties.put(Environment.TRANSACTION_STRATEGY,
                        JoinableCMTTransactionFactory.class.getName());
                properties.put(Environment.TRANSACTION_MANAGER_STRATEGY,
                        NuxeoTransactionManagerLookup.class.getName());
            }
        }
        return cfg.createEntityManagerFactory(properties);
    }

    /**
     * Hibernate Transaction Manager Lookup that uses our framework.
     */
    public static class NuxeoTransactionManagerLookup implements
            TransactionManagerLookup {
        public NuxeoTransactionManagerLookup() {
            // look up UserTransaction once to know its JNDI name
            try {
                TransactionHelper.lookupUserTransaction();
            } catch (NamingException e) {
                // ignore
            }
        }

        public TransactionManager getTransactionManager(Properties props) {
            try {
                return TransactionHelper.lookupTransactionManager();
            } catch (NamingException e) {
                throw new HibernateException(e.getMessage(), e);
            }
        }

        public String getUserTransactionName() {
            return TransactionHelper.getUserTransactionJNDIName();
        }
    }

    public EntityManagerFactory getFactory() {
        return getFactory(null);
    }

    public static String getTxType() {
        String txType;
        if (Framework.isInitialized()) {
            txType = Framework.getProperty(TXTYPE_PROPERTY_NAME);
            if (txType == null) {
                try {
                    TransactionHelper.lookupTransactionManager();
                    txType = JTA;
                } catch (NamingException e) {
                    txType = RESOURCE_LOCAL;
                }
            }
        } else {
            txType = RESOURCE_LOCAL;
        }
        return txType;
    }

    public static HibernateConfiguration load(URL location) {
        XMap map = new XMap();
        map.register(HibernateConfiguration.class);
        try {
            return (HibernateConfiguration) map.load(location);
        } catch (Exception e) {
            throw new PersistenceError(
                    "Cannot load hibernate configuration from " + location, e);
        }
    }

    public void merge(HibernateConfiguration other) {
        assert name.equals(other.name) : " cannot merge configuration that do not have the same persistence unit";
        annotedClasses.addAll(other.annotedClasses);
        hibernateProperties.clear();
        hibernateProperties.putAll(other.hibernateProperties);
    }

}
