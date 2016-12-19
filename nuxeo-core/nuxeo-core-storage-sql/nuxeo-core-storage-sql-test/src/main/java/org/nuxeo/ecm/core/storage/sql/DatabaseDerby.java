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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import java.io.File;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.commons.io.FileUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Florent Guillaume
 */
public class DatabaseDerby extends DatabaseHelper {

    public static final DatabaseHelper INSTANCE = new DatabaseDerby();

    /** This directory will be deleted and recreated. */
    private static final String DIRECTORY = "target/test/derby";

    private static final String DEF_USER = "sa";

    private static final String DEF_PASSWORD = "";

    private static final String CONTRIB_XML = "OSGI-INF/test-repo-repository-derby-contrib.xml";

    private static final String LOG = "target/test/derby.log";

    private static final String DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

    protected String url;

    protected void setProperties() {
        setProperty(DATABASE_PROPERTY, new File(DIRECTORY).getAbsolutePath());
        setProperty(USER_PROPERTY, DEF_USER);
        setProperty(PASSWORD_PROPERTY, DEF_PASSWORD);
        // for sql directory tests
        setProperty(DRIVER_PROPERTY, DRIVER);
        url = String.format("jdbc:derby:%s;create=true", Framework.getProperty(DATABASE_PROPERTY));
        setProperty(URL_PROPERTY, url);
    }

    @Override
    public void setUp() throws SQLException {
        super.setUp();
        System.setProperty("derby.stream.error.file", new File(LOG).getAbsolutePath());
        // newInstance needed after a previous shutdown
        try {
            Class.forName(DRIVER).newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        File dbdir = new File(DIRECTORY);
        File parent = dbdir.getParentFile();
        FileUtils.deleteQuietly(dbdir);
        parent.mkdirs();
        // the following noticeably improves performance
        System.setProperty("derby.system.durability", "test");
        setProperties();
    }

    @Override
    public void tearDown() throws SQLException {
        Exception ex = null;
        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
            // after this to reboot the driver a newInstance is needed
        } catch (SQLException e) {
            String message = e.getMessage();
            if ("Derby system shutdown.".equals(message)) {
                return;
            }
            if ("org.apache.derby.jdbc.EmbeddedDriver is not registered with the JDBC driver manager".equals(message)) {
                // huh? happens for testClustering
                return;
            }
            ex = e;
        } finally {
            super.tearDown();
        }
        throw new RuntimeException("Expected Derby shutdown exception instead", ex);
    }

    @Override
    public String getDeploymentContrib() {
        return CONTRIB_XML;
    }

    @Override
    public RepositoryDescriptor getRepositoryDescriptor() {
        return new RepositoryDescriptor();
    }

    @Override
    public boolean supportsMultipleFulltextIndexes() {
        return false;
    }

}
