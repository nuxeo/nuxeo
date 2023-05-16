/*
 * (C) Copyright 2006-2023 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.core.persistence;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.transaction.jta.platform.internal.AbstractJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.datasource.DataSourceHelper;
import org.nuxeo.runtime.jtajca.NamingContextFactory;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.transaction.TransactionHelper;

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

    @XNodeMap(value = "properties/property", key = "@name", type = HashMap.class, componentType = String.class)
    public final Map<String, String> hibernateProperties = new HashMap<>();

    @Override
    public EntityManagerFactory getFactory(String txType) {
        Map<String, String> properties = new HashMap<>();
        properties.putAll(hibernateProperties);

        if (txType == null) {
            txType = getTxType();
        }
        properties.put(AvailableSettings.JPA_TRANSACTION_TYPE, txType);
        // no need to set "hibernate.transaction.coordinator_class" (AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY)
        // as in Hibernate 3, it is now done by EntityManagerFactoryBuilderImpl#normalizeTransactionCoordinator
        if (txType.equals(JTA)) {
            properties.put(AvailableSettings.JTA_PLATFORM, NuxeoJtaPlatform.class.getName());
        }

        if (properties.get(AvailableSettings.URL) == null) {
            // don't set up our connection provider for unit tests
            // that use an explicit driver + connection URL and so use
            // a DriverManagerConnectionProvider
            properties.put(AvailableSettings.CONNECTION_PROVIDER, NuxeoConnectionProvider.class.getName());
        }
        if (txType.equals(RESOURCE_LOCAL)) {
            properties.remove(AvailableSettings.DATASOURCE);
        } else {
            String dsname = properties.get(AvailableSettings.DATASOURCE);
            dsname = DataSourceHelper.getDataSourceJNDIName(dsname);
            properties.put(AvailableSettings.DATASOURCE, dsname);
            properties.put(AvailableSettings.JNDI_CLASS, NamingContextFactory.class.getName());
            properties.put(AvailableSettings.JNDI_PREFIX.concat(".").concat(Context.URL_PKG_PREFIXES),
                    NuxeoContainer.class.getPackage().getName());
        }
        return Persistence.createEntityManagerFactory(name, properties);
    }

    /**
     * Hibernate {@link JtaPlatform} that uses our framework.
     *
     * @since 2023
     */
    public static class NuxeoJtaPlatform extends AbstractJtaPlatform {

        private static final long serialVersionUID = 1L;

        @Override
        protected TransactionManager locateTransactionManager() {
            try {
                return TransactionHelper.lookupTransactionManager();
            } catch (NamingException e) {
                throw new HibernateException(e.getMessage(), e);
            }
        }

        @Override
        protected UserTransaction locateUserTransaction() {
            try {
                return TransactionHelper.lookupUserTransaction();
            } catch (NamingException e) {
                throw new HibernateException(e.getMessage(), e);
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
        if (!name.equals(other.name)) {
            throw new NuxeoException("Cannot merge configurations that do not have the same persistence unit");
        }
        hibernateProperties.clear();
        hibernateProperties.putAll(other.hibernateProperties);
    }

}
