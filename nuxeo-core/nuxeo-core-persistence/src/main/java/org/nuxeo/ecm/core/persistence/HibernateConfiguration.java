/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stephane Lacoin
 *     Florent Guillaume
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
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.ejb.HibernatePersistence;
import org.hibernate.ejb.transaction.JoinableCMTTransaction;
import org.hibernate.ejb.transaction.JoinableCMTTransactionFactory;
import org.hibernate.jdbc.JDBCContext;
import org.hibernate.transaction.JDBCTransactionFactory;
import org.hibernate.transaction.TransactionFactory;
import org.hibernate.transaction.TransactionManagerLookup;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.datasource.ConnectionHelper;
import org.nuxeo.runtime.datasource.DataSourceHelper;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
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
        return setupConfiguration(null);
    }

    public Ejb3Configuration setupConfiguration(Map<String, String> properties) {
        cfg = new Ejb3Configuration();

        if (properties!=null) {
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
            Class<?> transactionFactoryClass;
            if (ConnectionHelper.useSingleConnection(null)) {
                transactionFactoryClass = NuxeoTransactionFactory.class;
            } else {
                transactionFactoryClass = JoinableCMTTransactionFactory.class;
            }
            properties.put(Environment.TRANSACTION_STRATEGY, transactionFactoryClass.getName());
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
            props.put(Environment.CONNECTION_PROVIDER,
                    NuxeoConnectionProvider.class.getName());
        }
        if (txType.equals(RESOURCE_LOCAL)) {
            props.remove(Environment.DATASOURCE);
        }
        return createEntityManagerFactory(properties);
    }

    /**
     * Don't close the connection aggressively after each statement.
     */
    // needs to be public as hibernate calls newInstance
    public static class NuxeoTransactionFactory extends
            JoinableCMTTransactionFactory {
        @Override
        public ConnectionReleaseMode getDefaultReleaseMode() {
            return ConnectionReleaseMode.AFTER_TRANSACTION;
        }

        @Override
        public org.hibernate.Transaction createTransaction(
                JDBCContext jdbcContext,
                TransactionFactory.Context transactionContext)
                throws HibernateException {
            return new NuxeoHibernateTransaction(jdbcContext,
                    transactionContext);
        }
    }

    /**
     * Hibernate transaction that will register a synchronization that runs
     * before the one from ConnectionHelper in single-datasource mode.
     * <p>
     * Needed because the sync from org.hibernate.ejb.EntityManagerImpl#close
     * must run before the one from ConnectionHelper.
     */
    public static class NuxeoHibernateTransaction extends
            JoinableCMTTransaction {
        public NuxeoHibernateTransaction(JDBCContext jdbcContext,
                TransactionFactory.Context transactionContext) {
            super(jdbcContext, transactionContext);
        }

        @Override
        public void registerSynchronization(Synchronization sync)
                throws HibernateException {
            boolean registered;
            try {
                registered = ConnectionHelper.registerSynchronization(sync);
            } catch (SystemException e) {
                throw new HibernateException(e);
            }
            if (!registered) {
                super.registerSynchronization(sync);
            }
        }
    }

    // this must be executed always outside a transaction
    // because SchemaUpdate tries to setAutoCommit(true)
    // so we use a new thread
    protected EntityManagerFactory createEntityManagerFactory(
            final Map<String, String> properties) {
        final EntityManagerFactory[] emf = new EntityManagerFactory[1];
        Thread t = new Thread("persistence-init-"+name) {
            @SuppressWarnings("deprecation")
            @Override
            public void run() {
                emf[0] = cfg.createEntityManagerFactory(properties);
            };
        };
        try {
            t.start();
            t.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return emf[0];
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
