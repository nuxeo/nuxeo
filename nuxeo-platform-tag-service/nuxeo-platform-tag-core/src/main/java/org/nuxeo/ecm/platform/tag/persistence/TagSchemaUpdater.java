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

import static org.nuxeo.ecm.platform.tag.entity.TaggingConstants.TAGGING_TABLE_COLUMN_AUTHOR;
import static org.nuxeo.ecm.platform.tag.entity.TaggingConstants.TAGGING_TABLE_COLUMN_CREATION_DATE;
import static org.nuxeo.ecm.platform.tag.entity.TaggingConstants.TAGGING_TABLE_COLUMN_DOCUMENT_ID;
import static org.nuxeo.ecm.platform.tag.entity.TaggingConstants.TAGGING_TABLE_COLUMN_ID;
import static org.nuxeo.ecm.platform.tag.entity.TaggingConstants.TAGGING_TABLE_COLUMN_IS_PRIVATE;
import static org.nuxeo.ecm.platform.tag.entity.TaggingConstants.TAGGING_TABLE_COLUMN_TAG_ID;
import static org.nuxeo.ecm.platform.tag.entity.TaggingConstants.TAGGING_TABLE_NAME;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.Settings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DialectFactory;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.ejb.packaging.PersistenceMetadata;
import org.hibernate.ejb.packaging.PersistenceXmlLoader;
import org.hibernate.impl.SessionImpl;
import org.nuxeo.ecm.platform.tag.sql.Column;
import org.nuxeo.ecm.platform.tag.sql.Table;

/**
 * @author "Stephane Lacoin (aka matic) <slacoin@nuxeo.com>"
 *
 */
public class TagSchemaUpdater {

    public static final Log log = LogFactory.getLog(TagSchemaUpdater.class);

    public final AnnotationConfiguration configuration = new AnnotationConfiguration();

    public final PersistenceMetadata metadata = doLoadMetadata();

    public final Properties connectionProperties = new Properties();

    public TagSchemaUpdater() {
        doSetup();
    }

    public TagSchemaUpdater(Properties properties) {
        connectionProperties.putAll(properties);
        doSetup();
    }

    protected void doSetup() {
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
        if (connectionProperties.get(Environment.URL) != null) {
            return;
        }
        String jtaDatasource = metadata.getJtaDatasource();
        connectionProperties.setProperty(Environment.DATASOURCE, jtaDatasource);
    }

    protected PersistenceMetadata doLoadMetadata() {
        Enumeration<URL> xmls;
        try {
            xmls = TagSchemaUpdater.class.getClassLoader().getResources("META-INF/persistence.xml");
        } catch (IOException e1) {
            throw new Error("No persistence.xml files in class path", e1);
        }
        while (xmls.hasMoreElements()) {
            URL url = xmls.nextElement();
            List<PersistenceMetadata> metadataFiles = null;
            try {
                metadataFiles = PersistenceXmlLoader.deploy(
                        url, Collections.EMPTY_MAP, configuration.getEntityResolver());
            } catch (Exception e) {
                log.warn("Cannot load " + url);
                continue;
            }
            for (PersistenceMetadata metadata : metadataFiles) {
                if (metadata.getName().equals("nxtags")) {
                    return metadata;
                }
            }
        }
        throw new Error("cannot find nxtags persistence unit");
    }

    public static Dialect determineDialect(SessionImpl session) {
        try {
            DatabaseMetaData meta = session.getFactory().getConnectionProvider().getConnection().getMetaData();
            return DialectFactory.determineDialect(meta.getDatabaseProductName(), meta.getDatabaseMajorVersion());
        } catch (SQLException e) {
            throw new Error("Cannot determine dialect", e);
        }
    }

    public static class CustomPostgreSQLDialect extends org.hibernate.dialect.PostgreSQLDialect {
        public CustomPostgreSQLDialect() {
            super();
            registerColumnType(Types.BOOLEAN, "boolean");
            registerHibernateType(Types.BOOLEAN, "boolean");
        }
    }

    public void update() {
        configuration.setProperties(connectionProperties);
        Settings settings = configuration.buildSettings();
        Table table = new Table(TAGGING_TABLE_NAME);
        Column column = new Column(TAGGING_TABLE_COLUMN_ID, Types.VARCHAR);
        column.setPrimary(true);
        column.setNullable(false);
        table.addColumn(column);
        column = new Column(TAGGING_TABLE_COLUMN_TAG_ID, Types.CLOB);
        table.addColumn(column);
        column = new Column(TAGGING_TABLE_COLUMN_AUTHOR, Types.VARCHAR);
        table.addColumn(column);
        column = new Column(TAGGING_TABLE_COLUMN_DOCUMENT_ID, Types.CLOB);
        table.addColumn(column);
        column = new Column(TAGGING_TABLE_COLUMN_CREATION_DATE, Types.DATE);
        table.addColumn(column);
        column = new Column(TAGGING_TABLE_COLUMN_IS_PRIVATE, Types.BOOLEAN);
        table.addColumn(column);
        Dialect dialect = settings.getDialect();
        if (dialect instanceof PostgreSQLDialect) {
            dialect = new CustomPostgreSQLDialect();
        }
        String script = table.getCreateSql(dialect);
        try {
            Connection connection = settings.getConnectionProvider().getConnection();
            Statement statement = connection.createStatement();
            statement.execute(script);
        } catch (SQLException e) {
            throw new Error("Cannot update schema", e);
        }
    }

}
