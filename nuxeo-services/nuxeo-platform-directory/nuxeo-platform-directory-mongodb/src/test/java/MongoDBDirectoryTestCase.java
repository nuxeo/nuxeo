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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.nuxeo.directory.mongodb.MongoDBDirectory;
import org.nuxeo.directory.mongodb.MongoDBSession;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * @since 9.1
 */
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.directory.mongodb", "org.nuxeo.ecm.directory", "org.nuxeo.ecm.directory.api",
        "org.nuxeo.ecm.directory.types.contrib", "org.nuxeo.ecm.directory.sql" })
@ConditionalIgnoreRule.Ignore(condition = IgnoreNoMongoDB.class, cause = "Needs a MongoDB server!")
public abstract class MongoDBDirectoryTestCase {

    protected static final String CONTINENT_DIR = "testContinent";

    protected static final String GROUP_DIR = "groupDirectory";

    protected static final String USER_DIR = "userDirectory";

    protected static final String TEST_DIR = "testDirectory";

    protected Map<String, Object> testContinent;

    @Inject
    protected DirectoryService directoryService;


    protected MongoDBDirectory getDirectory(String directory) {
        return (MongoDBDirectory) directoryService.getDirectory(directory);
    }

    protected Session openSession(String directory) {
        return directoryService.open(directory);
    }

    protected MongoDBDirectoryTestCase() {
        testContinent = new HashMap<>();
        testContinent.put("id", "europe");
        testContinent.put("label", "label.directories.continent.europe");
        testContinent.put("obsolete", 0);
        testContinent.put("ordering", 0);
    }

    @After
    public void tearDown() {
        purgeDirectory(CONTINENT_DIR);
        purgeDirectory(GROUP_DIR);
        purgeDirectory(USER_DIR);
        purgeDirectory(TEST_DIR);
    }

    private void purgeDirectory(String directory) {
        MongoDBDirectory dir = getDirectory(directory);
        if (dir != null) {
            try (MongoDBSession session = (MongoDBSession) dir.getSession()) {
                session.query(Collections.emptyMap()).forEach(session::deleteEntry);
                session.getCollection().drop();
            }
        }
    }
}
