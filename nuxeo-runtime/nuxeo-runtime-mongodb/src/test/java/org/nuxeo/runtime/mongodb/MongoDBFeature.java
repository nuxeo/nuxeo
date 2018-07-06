/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.runtime.mongodb;

import java.io.IOException;
import java.net.URL;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.URLStreamRef;
import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.osgi.framework.Bundle;

import com.mongodb.MongoClient;

/**
 * @since 9.1
 */
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.runtime.mongodb")
@Deploy("org.nuxeo.runtime.mongodb.test")
@ConditionalIgnoreRule.Ignore(condition = IgnoreNoMongoDB.class, cause = "Needs a MongoDB server!")
public class MongoDBFeature implements RunnerFeature {

    public static final String MONGODB_SERVER_PROPERTY = "nuxeo.test.mongodb.server";

    public static final String MONGODB_DBNAME_PROPERTY = "nuxeo.test.mongodb.dbname";

    public static final String DEFAULT_MONGODB_SERVER = "localhost:27017";

    public static final String DEFAULT_MONGODB_DBNAME = "unittests";

    protected static String defaultProperty(String name, String def) {
        String value = System.getProperty(name);
        if (value == null || value.equals("") || value.equals("${" + name + "}")) {
            value = def;
        }
        Framework.getProperties().setProperty(name, value);
        return value;
    }

    @Override
    public void start(FeaturesRunner runner) {
        // first configure the default MongoDB connection
        String server = defaultProperty(MONGODB_SERVER_PROPERTY, DEFAULT_MONGODB_SERVER);
        String dbname = defaultProperty(MONGODB_DBNAME_PROPERTY, DEFAULT_MONGODB_DBNAME);
        // deploy the test bundle after the default properties have been set
        try {
            RuntimeHarness harness = runner.getFeature(RuntimeFeature.class).getHarness();
            Bundle bundle = harness.getOSGiAdapter().getRegistry().getBundle("org.nuxeo.runtime.mongodb.test");
            URL url = bundle.getEntry("OSGI-INF/mongodb-test-contrib.xml");
            harness.getContext().deploy(new URLStreamRef(url));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // finally clear the DB
        try (MongoClient client = MongoDBConnectionHelper.newMongoClient(server)) {
            MongoDBConnectionHelper.getDatabase(client, dbname).drop();
        }
    }

}
