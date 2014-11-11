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
package org.nuxeo.ecm.platform.tag;

import java.sql.SQLException;
import java.util.Map;

import org.hibernate.cfg.Environment;
import org.nuxeo.ecm.core.storage.sql.DatabaseH2;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.tag.persistence.TagSchemaUpdater;


/**
 * @author "Stephane Lacoin (aka matic) <slacoin@nuxeo.com>"
 */
public class TestSchemaUpdater extends SQLRepositoryTestCase {

    {
        database = DatabaseH2.INSTANCE;
    }

    TagSchemaUpdater updater = new TagSchemaUpdater();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        openSession();
    }

    public void testUpdate() throws SQLException {
        updater.configuration.getClassMappings();
        updater.connectionProperties.clear();
        updater.connectionProperties.setProperty(Environment.DIALECT, H2WithNoSequencesSupportDialect.class.getCanonicalName());
        Map<String, String> repositoryProperties = database.getRepositoryDescriptor().properties;
        updater.connectionProperties.setProperty(Environment.DRIVER, "org.h2.Driver");
        String connectionURL = repositoryProperties.get("URL").replace("${nuxeo.test.vcs.url}", System.getProperty("nuxeo.test.vcs.url"));
        updater.connectionProperties.setProperty(Environment.URL, connectionURL);
        updater.connectionProperties.setProperty(Environment.USER, repositoryProperties.get("User"));
        updater.connectionProperties.setProperty(Environment.PASS, repositoryProperties.get("Password"));
        updater.update();
    }

}
