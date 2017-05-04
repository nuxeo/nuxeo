/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.directory.test;

import com.mongodb.MongoClient;
import org.nuxeo.directory.mongodb.MongoDBConnectionHelper;
import org.nuxeo.ecm.core.test.StorageConfiguration;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

/**
 * Description of the specific capabilities of a directory for tests, and helper methods.
 *
 * @since 9.2
 */
public class DirectoryConfiguration {

    public static final String DIRECTORY_PROPERTY = "nuxeo.test.directory";

    public static final String DIRECTORY_VCS = "vcs";

    public static final String DIRECTORY_MONGODB = "mongodb";

    public static final String DIRECTORY_LDAP = "ldap";

    public static final String DIRECTORY_MULTI = "multi";

    protected String directoryType;

    protected StorageConfiguration storageConfiguration;

    public DirectoryConfiguration(StorageConfiguration storageConfiguration) {
        this.storageConfiguration = storageConfiguration;
        directoryType = StorageConfiguration.defaultSystemProperty(DIRECTORY_PROPERTY,
                storageConfiguration.getCoreType());
    }

    public String deployBundles(FeaturesRunner runner) throws Exception {
        String bundleName = null;
        switch (directoryType) {
        case DIRECTORY_VCS:
            bundleName = "org.nuxeo.ecm.directory.sql";
            break;
        case DIRECTORY_MONGODB:
            bundleName = "org.nuxeo.directory.mongodb";
            break;
        case DIRECTORY_LDAP:
            bundleName = "org.nuxeo.ecm.directory.ldap";
            break;
        case DIRECTORY_MULTI:
            bundleName = "org.nuxeo.ecm.directory.multi";
            break;
        default:
            break;
        }
        String testBundleName = bundleName + ".tests";

        RuntimeHarness harness = runner.getFeature(RuntimeFeature.class).getHarness();
        harness.deployBundle(bundleName);
        harness.deployBundle(testBundleName);

        return testBundleName;
    }

    public void init() {
        switch (directoryType) {
        case DIRECTORY_VCS:
            if (!storageConfiguration.isVCS()) {
                storageConfiguration.initJDBC();
            }
            break;
        case DIRECTORY_MONGODB:
            initMongoDB();
            break;
        // TODO
        case DIRECTORY_LDAP:
        case DIRECTORY_MULTI:
        default:
            break;
        }
    }

    protected void initMongoDB() {
        String server = storageConfiguration.getMongoDBServer();
        String dbname = storageConfiguration.getMongoDBDbName();
        MongoClient mongoClient = MongoDBConnectionHelper.newMongoClient(server);
        try {
            MongoDBConnectionHelper.getCollection(mongoClient, dbname, "userDirectory").drop();
            MongoDBConnectionHelper.getCollection(mongoClient, dbname, "groupDirectory").drop();
        } finally {
            mongoClient.close();
        }
    }

}
