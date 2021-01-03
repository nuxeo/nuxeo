/*
 * (C) Copyright 2006-2021 Nuxeo (http://nuxeo.com/) and others.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.transaction.jta.platform.internal.AbstractJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatformException;
import org.hibernate.jpa.HibernatePersistenceProvider;
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
        hibernateProperties.put(AvailableSettings.DATASOURCE, DataSourceHelper.getDataSourceJNDIName(name));
    }

    @XNodeMap(value = "properties/property", key = "@name", type = Properties.class, componentType = String.class)
    public final Properties hibernateProperties = new Properties();

    @XNodeList(value = "classes/class", type = ArrayList.class, componentType = Class.class)
    public final List<Class<?>> annotedClasses = new ArrayList<>();

    public void addAnnotedClass(Class<?> annotedClass) {
        annotedClasses.add(annotedClass);
    }

    public void removeAnnotedClass(Class<?> annotedClass) {
        annotedClasses.remove(annotedClass);
    }

    @Override
    public EntityManagerFactory getFactory(String txType) {
        Map<String, Object> properties = new HashMap<>();
        // hibernate properties
        properties.putAll((Map<String, Object>) (Map) hibernateProperties);
        // annotated classes
        properties.put(AvailableSettings.LOADED_CLASSES, annotedClasses);
        if (txType == null) {
            txType = getTxType();
        }
        properties.put(AvailableSettings.JPA_TRANSACTION_TYPE, txType);
        if (txType.equals(JTA)) {
            properties.put(AvailableSettings.JTA_PLATFORM, new NuxeoJtaPlatform());
            String dsname = (String) properties.get(AvailableSettings.DATASOURCE);
            dsname = DataSourceHelper.getDataSourceJNDIName(dsname);
            properties.put(AvailableSettings.DATASOURCE, dsname);
            properties.put(AvailableSettings.JNDI_CLASS, NamingContextFactory.class.getName());
            properties.put(AvailableSettings.JNDI_PREFIX + "." + javax.naming.Context.URL_PKG_PREFIXES,
                    NuxeoContainer.class.getPackage().getName());
        } else {
            properties.remove(AvailableSettings.DATASOURCE);
        }
        if (properties.get(AvailableSettings.URL) == null) {
            // don't set up our connection provider for unit tests
            // that use an explicit driver + connection URL and so use
            // a DriverManagerConnectionProviderImpl
            properties.put(AvailableSettings.CONNECTION_PROVIDER, new NuxeoConnectionProvider());
        }
        return new HibernatePersistenceProvider().createEntityManagerFactory(name, properties);
    }

    /**
     * Hibernate JTA Platform that uses our framework.
     *
     * @since 11.5
     */
    public static class NuxeoJtaPlatform extends AbstractJtaPlatform {

        private static final long serialVersionUID = 1L;

        @Override
        protected TransactionManager locateTransactionManager() {
            try {
                return TransactionHelper.lookupTransactionManager();
            } catch (NamingException e) {
                throw new JtaPlatformException(e.getMessage(), e);
            }
        }

        @Override
        protected UserTransaction locateUserTransaction() {
            try {
                return TransactionHelper.lookupUserTransaction();
            } catch (NamingException e) {
                throw new JtaPlatformException(e.getMessage(), e);
            }
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
