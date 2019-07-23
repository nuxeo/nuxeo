/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     Stephane Lacoin
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.persistence;

import java.io.IOException;
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
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.ejb.HibernatePersistence;
import org.hibernate.ejb.transaction.JoinableCMTTransactionFactory;
import org.hibernate.transaction.JDBCTransactionFactory;
import org.hibernate.transaction.TransactionManagerLookup;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.datasource.DataSourceHelper;
import org.nuxeo.runtime.jtajca.NamingContextFactory;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 */
@XObject("hibernateConfiguration")
public class HibernateConfiguration implements EntityManagerFactoryProvider {

    public static final String RESOURCE_LOCAL = PersistenceUnitTransactionType.RESOURCE_LOCAL.name();

    public static final String JTA = PersistenceUnitTransactionType.JTA.name();

    public static final String TXTYPE_PROPERTY_NAME = "org.nuxeo.runtime.txType";

    @XNode("@name")
    public String name;

    @XNode("datasource")
    public void setDatasource(String name) {
        String expandedValue = Framework.expandVars(name);
        if (expandedValue.startsWith("$")) {
            throw new PersistenceError("Cannot expand " + name + " for datasource");
        }
        hibernateProperties.put("hibernate.connection.datasource", DataSourceHelper.getDataSourceJNDIName(name));
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
        return setupConfiguration(null);
    }

    public Ejb3Configuration setupConfiguration(Map<String, String> properties) {
        cfg = new Ejb3Configuration();

        if (properties != null) {
            cfg.configure(name, properties);
        } else {
            cfg.configure(name, Collections.emptyMap());
        }

        // Load hibernate properties
        cfg.addProperties(hibernateProperties);

        // Add annnoted classes if any
        for (Class<?> annotedClass : annotedClasses) {
            cfg.addAnnotatedClass(annotedClass);
        }

        return cfg;
    }

    @Override
    public EntityManagerFactory getFactory(String txType) {
        Map<String, String> properties = new HashMap<String, String>();
        if (txType == null) {
            txType = getTxType();
        }
        properties.put(HibernatePersistence.TRANSACTION_TYPE, txType);
        if (txType.equals(JTA)) {
            properties.put(Environment.TRANSACTION_STRATEGY, JoinableCMTTransactionFactory.class.getName());
            properties.put(Environment.TRANSACTION_MANAGER_STRATEGY, NuxeoTransactionManagerLookup.class.getName());
        } else if (txType.equals(RESOURCE_LOCAL)) {
            properties.put(Environment.TRANSACTION_STRATEGY, JDBCTransactionFactory.class.getName());
        }
        if (cfg == null) {
            setupConfiguration(properties);
        }
        Properties props = cfg.getProperties();
        if (props.get(Environment.URL) == null) {
            // don't set up our connection provider for unit tests
            // that use an explicit driver + connection URL and so use
            // a DriverManagerConnectionProvider
            props.put(Environment.CONNECTION_PROVIDER, NuxeoConnectionProvider.class.getName());
        }
        if (txType.equals(RESOURCE_LOCAL)) {
            props.remove(Environment.DATASOURCE);
        } else {
            String dsname = props.getProperty(Environment.DATASOURCE);
            dsname = DataSourceHelper.getDataSourceJNDIName(dsname);
            props.put(Environment.DATASOURCE, dsname);
            props.put(Environment.JNDI_CLASS, NamingContextFactory.class.getName());
            props.put(Environment.JNDI_PREFIX.concat(".").concat(javax.naming.Context.URL_PKG_PREFIXES),
                    NuxeoContainer.class.getPackage().getName());
        }
        return createEntityManagerFactory(properties);
    }

    // this must be executed always outside a transaction
    // because SchemaUpdate tries to setAutoCommit(true)
    // so we use a new thread
    protected EntityManagerFactory createEntityManagerFactory(final Map<String, String> properties) {
        return TransactionHelper.runWithoutTransaction(() -> cfg.createEntityManagerFactory(properties));
    }

    /**
     * Hibernate Transaction Manager Lookup that uses our framework.
     */
    public static class NuxeoTransactionManagerLookup implements TransactionManagerLookup {
        public NuxeoTransactionManagerLookup() {
            // look up UserTransaction once to know its JNDI name
            try {
                TransactionHelper.lookupUserTransaction();
            } catch (NamingException e) {
                // ignore
            }
        }

        @Override
        public TransactionManager getTransactionManager(Properties props) {
            try {
                return TransactionHelper.lookupTransactionManager();
            } catch (NamingException e) {
                throw new HibernateException(e.getMessage(), e);
            }
        }

        @Override
        public String getUserTransactionName() {
            return TransactionHelper.getUserTransactionJNDIName();
        }

        @Override
        public Object getTransactionIdentifier(Transaction transaction) {
            return transaction;
        }
    }

    @Override
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
        } catch (IOException e) {
            throw new PersistenceError("Cannot load hibernate configuration from " + location, e);
        }
    }

    public void merge(HibernateConfiguration other) {
        assert name.equals(other.name) : " cannot merge configuration that do not have the same persistence unit";
        annotedClasses.addAll(other.annotedClasses);
        hibernateProperties.clear();
        hibernateProperties.putAll(other.hibernateProperties);
    }

}
