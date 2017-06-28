/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.test;

import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.storage.dbs.DBSHelper;
import org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository;
import org.nuxeo.ecm.core.storage.mongodb.MongoDBRepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.DatabaseDB2;
import org.nuxeo.ecm.core.storage.sql.DatabaseDerby;
import org.nuxeo.ecm.core.storage.sql.DatabaseH2;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.core.storage.sql.DatabaseMySQL;
import org.nuxeo.ecm.core.storage.sql.DatabaseOracle;
import org.nuxeo.ecm.core.storage.sql.DatabasePostgreSQL;
import org.nuxeo.ecm.core.storage.sql.DatabaseSQLServer;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.osgi.framework.Bundle;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;

/**
 * Description of the specific capabilities of a repository for tests, and helper methods.
 *
 * @since 7.3
 */
public class StorageConfiguration {

    private static final Log log = LogFactory.getLog(StorageConfiguration.class);

    public static final String CORE_PROPERTY = "nuxeo.test.core";

    public static final String CORE_VCS = "vcs";

    public static final String CORE_MEM = "mem";

    public static final String CORE_MONGODB = "mongodb";

    public static final String CORE_MARKLOGIC = "marklogic";

    public static final String DEFAULT_CORE = CORE_VCS;

    private static final String MONGODB_SERVER_PROPERTY = "nuxeo.test.mongodb.server";

    private static final String MONGODB_DBNAME_PROPERTY = "nuxeo.test.mongodb.dbname";

    public static final String DEFAULT_MONGODB_SERVER = "localhost:27017";

    public static final String DEFAULT_MONGODB_DBNAME = "unittests";

    private static final String CHANGE_TOKEN_ENABLED_PROPERTY = "nuxeo.test.changetoken.enabled";

    private static final String CHANGE_TOKEN_ENABLED_DEFAULT = "true";

    private String coreType;

    private boolean isVCS;

    private boolean isDBS;

    private DatabaseHelper databaseHelper;

    private DBSHelper dbsHelper;

    final CoreFeature feature;

    private boolean changeTokenEnabled;

    protected String mongoDBServer;

    protected String mongoDBDbName;

    public StorageConfiguration(CoreFeature feature) {
        coreType = defaultSystemProperty(CORE_PROPERTY, DEFAULT_CORE);
        this.feature = feature;
    }

    public static String defaultSystemProperty(String name, String def) {
        String value = System.getProperty(name);
        if (value == null || value.equals("") || value.equals("${" + name + "}")) {
            System.setProperty(name, value = def);
        }
        return value;
    }

    protected static String defaultProperty(String name, String def) {
        String value = System.getProperty(name);
        if (value == null || value.equals("") || value.equals("${" + name + "}")) {
            value = def;
        }
        Framework.getProperties().setProperty(name, value);
        return value;
    }

    protected void init() {
        changeTokenEnabled = Boolean.parseBoolean(
                defaultProperty(CHANGE_TOKEN_ENABLED_PROPERTY, CHANGE_TOKEN_ENABLED_DEFAULT));
        initJDBC();
        switch (coreType) {
        case CORE_VCS:
            isVCS = true;
            break;
        case CORE_MEM:
            isDBS = true;
            break;
        case CORE_MONGODB:
            isDBS = true;
            initMongoDB();
            break;
        default:
            isDBS = true;
            initExternal();
        }
    }

    public void initJDBC() {
        databaseHelper = DatabaseHelper.DATABASE;

        String msg = "Deploying JDBC using " + databaseHelper.getClass().getSimpleName();
        // System.out used on purpose, don't remove
        System.out.println(getClass().getSimpleName() + ": " + msg);
        log.info(msg);

        // setup system properties for generic XML extension points
        // this is used both for VCS (org.nuxeo.ecm.core.storage.sql.RepositoryService)
        // and DataSources (org.nuxeo.runtime.datasource) extension points
        try {
            databaseHelper.setUp();
        } catch (SQLException e) {
            throw new NuxeoException(e);
        }
    }

    protected void initMongoDB() {
        mongoDBServer = defaultProperty(MONGODB_SERVER_PROPERTY, DEFAULT_MONGODB_SERVER);
        mongoDBDbName = defaultProperty(MONGODB_DBNAME_PROPERTY, DEFAULT_MONGODB_DBNAME);
        MongoDBRepositoryDescriptor descriptor = new MongoDBRepositoryDescriptor();
        descriptor.name = getRepositoryName();
        descriptor.server = mongoDBServer;
        descriptor.dbname = mongoDBDbName;
        try {
            clearMongoDB(descriptor);
        } catch (UnknownHostException e) {
            throw new NuxeoException(e);
        }
    }

    protected void clearMongoDB(MongoDBRepositoryDescriptor descriptor) throws UnknownHostException {
        MongoClient mongoClient = MongoDBRepository.newMongoClient(descriptor);
        try {
            DBCollection coll = MongoDBRepository.getCollection(descriptor, mongoClient);
            coll.dropIndexes();
            coll.remove(new BasicDBObject());
            coll = MongoDBRepository.getCountersCollection(descriptor, mongoClient);
            coll.dropIndexes();
            coll.remove(new BasicDBObject());
        } finally {
            mongoClient.close();
        }
    }

    protected void initExternal() {
        // Get DBSHelper by reflection
        String className = String.format("org.nuxeo.ecm.core.storage.%s.DBSHelperImpl", coreType);
        try {
            dbsHelper = (DBSHelper) Class.forName(className).newInstance();
            dbsHelper.init();
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException("DBSHelperImpl not found: " + className, e);
        }
    }

    public boolean isVCS() {
        return isVCS;
    }

    public boolean isVCSH2() {
        return isVCS && databaseHelper instanceof DatabaseH2;
    }

    public boolean isVCSDerby() {
        return isVCS && databaseHelper instanceof DatabaseDerby;
    }

    public boolean isVCSPostgreSQL() {
        return isVCS && databaseHelper instanceof DatabasePostgreSQL;
    }

    public boolean isVCSMySQL() {
        return isVCS && databaseHelper instanceof DatabaseMySQL;
    }

    public boolean isVCSOracle() {
        return isVCS && databaseHelper instanceof DatabaseOracle;
    }

    public boolean isVCSSQLServer() {
        return isVCS && databaseHelper instanceof DatabaseSQLServer;
    }

    public boolean isVCSDB2() {
        return isVCS && databaseHelper instanceof DatabaseDB2;
    }

    public boolean isDBS() {
        return isDBS;
    }

    public boolean isDBSMem() {
        return isDBS && CORE_MEM.equals(coreType);
    }

    public boolean isDBSMongoDB() {
        return isDBS && CORE_MONGODB.equals(coreType);
    }

    public boolean isDBSExternal() {
        return dbsHelper != null;
    }

    public boolean isDBSMarkLogic() {
        return isDBS && CORE_MARKLOGIC.equals(coreType);
    }

    public String getRepositoryName() {
        return "test";
    }

    /**
     * For databases that do asynchronous fulltext indexing, sleep a bit.
     */
    public void sleepForFulltext() {
        if (isVCS()) {
            databaseHelper.sleepForFulltext();
        } else {
            // DBS
        }
    }

    /**
     * Sleep a bit to get to the next millisecond, to have different timestamps.
     */
    public void maybeSleepToNextSecond() {
        try {
            Thread.sleep(1); // 1 millisecond
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore interrupted status
            throw new RuntimeException(e);
        }
    }

    public void waitForAsyncCompletion() {
        feature.waitForAsyncCompletion();
    }

    public void waitForFulltextIndexing() {
        waitForAsyncCompletion();
        sleepForFulltext();
    }

    /**
     * Checks if the database supports multiple fulltext indexes.
     */
    public boolean supportsMultipleFulltextIndexes() {
        if (isVCS()) {
            return databaseHelper.supportsMultipleFulltextIndexes();
        } else {
            return false; // DBS
        }
    }

    public List<String> getExternalBundles() {
        if (isDBSExternal()) {
            return Arrays.asList(String.format("org.nuxeo.ecm.core.storage.%s", coreType),
                    String.format("org.nuxeo.ecm.core.storage.%s.test", coreType));
        }
        return Collections.emptyList();
    }

    public URL getBlobManagerContrib(FeaturesRunner runner) {
        String bundleName = "org.nuxeo.ecm.core.test";
        String contribPath = "OSGI-INF/test-storage-blob-contrib.xml";
        RuntimeHarness harness = runner.getFeature(RuntimeFeature.class).getHarness();
        Bundle bundle = harness.getOSGiAdapter().getRegistry().getBundle(bundleName);
        URL contribURL = bundle.getEntry(contribPath);
        assertNotNull("deployment contrib " + contribPath + " not found", contribURL);
        return contribURL;
    }

    public URL getRepositoryContrib(FeaturesRunner runner) {
        String msg;
        if (isVCS()) {
            msg = "Deploying a VCS repository";
        } else if (isDBS()) {
            msg = "Deploying a DBS repository using " + coreType;
        } else {
            throw new NuxeoException("Unkown test configuration (not vcs/dbs)");
        }
        // System.out used on purpose, don't remove
        System.out.println(getClass().getSimpleName() + ": " + msg);
        log.info(msg);

        String contribPath;
        String bundleName;
        if (isVCS()) {
            bundleName = "org.nuxeo.ecm.core.storage.sql.test";
            contribPath = databaseHelper.getDeploymentContrib();
        } else {
            bundleName = "org.nuxeo.ecm.core.test";
            if (isDBSMem()) {
                contribPath = "OSGI-INF/test-storage-repo-mem-contrib.xml";
            } else if (isDBSMongoDB()) {
                contribPath = "OSGI-INF/test-storage-repo-mongodb-contrib.xml";
            } else if (isDBSExternal()) {
                bundleName = String.format("org.nuxeo.ecm.core.storage.%s.test", coreType);
                contribPath = "OSGI-INF/test-storage-repo-contrib.xml";
            } else {
                throw new NuxeoException("Unkown DBS test configuration (not mem/mongodb)");
            }
        }
        RuntimeHarness harness = runner.getFeature(RuntimeFeature.class).getHarness();
        Bundle bundle = harness.getOSGiAdapter().getRegistry().getBundle(bundleName);
        URL contribURL = bundle.getEntry(contribPath);
        assertNotNull("deployment contrib " + contribPath + " not found", contribURL);
        return contribURL;
    }

    public boolean isChangeTokenEnabled() {
        return changeTokenEnabled;
    }

    /**
     * @since 9.2
     */
    public String getCoreType() {
        return coreType;
    }

    /**
     * @since 9.2
     */
    public String getMongoDBServer() {
        return mongoDBServer;
    }

    /**
     * @since 9.2
     */
    public String getMongoDBDbName() {
        return mongoDBDbName;
    }

}
