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

    public static final String DIRECTORY_SQL = "sql";

    public static final String DIRECTORY_MONGODB = "mongodb";

    public static final String SQL_TEMPLATE_CONTRIB = "OSGI-INF/test-directory-sql-contrib.xml";

    public static final String MONGODB_TEMPLATE_CONTRIB = "OSGI-INF/test-directory-mongodb-contrib.xml";

    protected String directoryType;

    protected StorageConfiguration storageConfiguration;

    public DirectoryConfiguration(StorageConfiguration storageConfiguration) {
        this.storageConfiguration = storageConfiguration;
        directoryType = StorageConfiguration.defaultSystemProperty(DIRECTORY_PROPERTY,
                storageConfiguration.isVCS() ? DIRECTORY_SQL : storageConfiguration.getCoreType());
    }

    public void deployContrib(FeaturesRunner runner) throws Exception {
        String contribName;
        switch (directoryType) {
        case DIRECTORY_SQL:
            contribName = SQL_TEMPLATE_CONTRIB;
            break;
        case DIRECTORY_MONGODB:
            contribName = MONGODB_TEMPLATE_CONTRIB;
            break;
        default:
            // Fallback on SQL template directory by default if directoryType unknown
            contribName = SQL_TEMPLATE_CONTRIB;
            break;
        }

        RuntimeHarness harness = runner.getFeature(RuntimeFeature.class).getHarness();
        harness.deployContrib("org.nuxeo.ecm.directory.tests", contribName);
    }

    public void init() {
        // nothing for now as the storage configuration always initialize necessary properties for JDBC tests,
        // use this method for specific initialization (for example a mock LDAP server)
    }
}
