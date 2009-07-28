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
 *     "Stephane Lacoin (aka matic) <slacoin@nuxeo.com>"
 */
package org.nuxeo.ecm.platform.tag.persistence;

import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.persistence.PersistenceException;

import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Environment;
import org.hibernate.ejb.connection.InjectedDataSourceConnectionProvider;
import org.hibernate.ejb.packaging.PersistenceMetadata;
import org.hibernate.ejb.packaging.PersistenceXmlLoader;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.nuxeo.ecm.core.persistence.HibernateConfiguration;
import org.nuxeo.ecm.core.persistence.HibernateConfigurator;
import org.nuxeo.runtime.api.Framework;

/**
 * @author "Stephane Lacoin (aka matic) <slacoin@nuxeo.com>"
 * 
 */
public class TagSchemaUpdater {

    public final AnnotationConfiguration configuration = new AnnotationConfiguration();

    public final PersistenceMetadata metadata = doLoadMetadata();

    public final Properties connectionProperties = new Properties();

    {
        doSetupAnnotedClasses();
        doSetupConnectionProperties();
    }

    protected void doSetupAnnotedClasses() {
        for (String mappedClassName : metadata.getClasses()) {
            try {
                configuration.addAnnotatedClass(Class.forName(mappedClassName));
            } catch (Exception e) {
                throw new Error("Cannot load class " + mappedClassName, e);
            }
        }
    }

    protected void doSetupConnectionProperties() {
        HibernateConfigurator configurator = Framework.getLocalService(HibernateConfigurator.class);
        HibernateConfiguration configuration = configurator.getHibernateConfiguration("nxtags");
        connectionProperties.putAll(configuration.hibernateProperties);
        if (connectionProperties.get(Environment.URL) != null) {
            return;
        }
        String jtaDatasource = metadata.getJtaDatasource();
        connectionProperties.setProperty(Environment.DATASOURCE, jtaDatasource);
        connectionProperties.setProperty(Environment.CONNECTION_PROVIDER, InjectedDataSourceConnectionProvider.class.getName());
    }

    protected PersistenceMetadata doLoadMetadata() {
        try {
            Enumeration<URL> xmls = Thread.currentThread().getContextClassLoader().getResources("META-INF/persistence.xml");
            while (xmls.hasMoreElements()) {
                URL url = xmls.nextElement();
                List<PersistenceMetadata> metadataFiles = PersistenceXmlLoader.deploy(url, Collections.EMPTY_MAP, configuration.getEntityResolver());
                for (PersistenceMetadata metadata : metadataFiles) {
                    if (metadata.getName().equals("nxtags")) {
                        return metadata;
                    }
                }
            }
            throw new Error("cannot find nxtags persistence unit");
        } catch (Exception e) {
            if (e instanceof PersistenceException) {
                throw (PersistenceException) e;
            } else {
                throw new PersistenceException(e);
            }
        }
    }

    public void update() {
        SchemaUpdate update = new SchemaUpdate(configuration, connectionProperties);
        update.execute(false, true);
    }
}
