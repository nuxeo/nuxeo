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

import org.nuxeo.ecm.core.api.NuxeoException;
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
            contribName = "OSGI-INF/test-directory-sql-contrib.xml";
            break;
        case DIRECTORY_MONGODB:
            contribName = "OSGI-INF/test-directory-mongodb-contrib.xml";
            break;
        default:
            throw new NuxeoException("Contribution for testing directories cannot be null");
        }

        RuntimeHarness harness = runner.getFeature(RuntimeFeature.class).getHarness();
        harness.deployContrib("org.nuxeo.ecm.directory.tests", contribName);
    }

    public void init() {
        switch (directoryType) {
        case DIRECTORY_SQL:
            if (!storageConfiguration.isVCS()) {
                storageConfiguration.initJDBC();
            }
            break;
        default:
            break;
        }
    }
}
