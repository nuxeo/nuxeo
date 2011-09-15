package org.nuxeo.ecm.platform.audit;

import java.sql.Connection;
import java.util.Properties;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.DirectoryServiceImpl;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.sql.SQLDirectoryProxy;
import org.nuxeo.ecm.directory.sql.SimpleDataSource;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestSQLDirectories extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        DatabaseHelper.DATABASE.setUp();

        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.directory.sql");
        deployBundle("org.nuxeo.ecm.directory.types.contrib");

        deployBundle("org.nuxeo.ecm.platform.audit.api");
        deployContrib("org.nuxeo.ecm.platform.audit.tests", "OSGI-INF/test-directories-contrib.xml");
    }

    @Override
    public void tearDown() throws Exception {
        DatabaseHelper.DATABASE.tearDown();
        super.tearDown();
    }

    protected static Session getSession(String dirName) throws ClientException {
        DirectoryService dirService = Framework.getLocalService(DirectoryService.class);
        return dirService.open(dirName);
    }

    protected static Directory getDirectory(String dirName)
            throws DirectoryException {
        DirectoryServiceImpl dirServiceImpl = (DirectoryServiceImpl) Framework.getRuntime().getComponent(
                DirectoryService.NAME);
        Directory dir = dirServiceImpl.getDirectory(dirName);
        if (dir instanceof SQLDirectoryProxy) {
            dir = ((SQLDirectoryProxy) dir).getDirectory();
        }
        return dir;
    }

    public Connection getConnection() throws Exception {
        return new SimpleDataSource("jdbc:hsqldb:mem:memid",
                "org.hsqldb.jdbcDriver", "sa", "").getConnection();
    }

    public static void setUpContextFactory() {
        Properties props = System.getProperties();
        props.put("java.naming.factory.initial",
                "org.nuxeo.ecm.directory.sql.LocalContextFactory");
    }


    public void testDirectories() throws Exception {
        Directory eventDir = getDirectory("eventTypes");
        assertNotNull(eventDir);
        Directory categoryDir = getDirectory("eventCategories");
        assertNotNull(categoryDir);

    }
}
