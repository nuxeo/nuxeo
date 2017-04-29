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
 *     dmetzler
 */
package org.nuxeo.ecm.core.storage.mongodb;

import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.nuxeo.launcher.config.ConfigurationException;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.launcher.config.backingservices.BackingChecker;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoTimeoutException;
import com.mongodb.ServerAddress;

public class MongoDBChecker implements BackingChecker {

    public static final String TEMPLATE_NAME = "mongodb";

    @Override
    public boolean accepts(ConfigurationGenerator cg) {
        return cg.getTemplateList().contains(TEMPLATE_NAME);
    }

    @Override
    public void check(ConfigurationGenerator cg) throws ConfigurationException {
        MongoClient ret = null;
        String serverName = cg.getUserConfig().getProperty(ConfigurationGenerator.PARAM_MONGODB_SERVER);
        String dbName = cg.getUserConfig().getProperty(ConfigurationGenerator.PARAM_MONGODB_NAME);

        MongoClientOptions.Builder optionsBuilder = MongoClientOptions.builder()
                                                                      .serverSelectionTimeout(
                                                                              (int) TimeUnit.SECONDS.toMillis(1))
                                                                      .description("Nuxeo DB Check");
        if (serverName.startsWith("mongodb://")) {
            // allow mongodb:// URI syntax for the server, to pass everything in one string
            ret = new MongoClient(new MongoClientURI(serverName, optionsBuilder));
        } else {
            ret = new MongoClient(new ServerAddress(serverName), optionsBuilder.build());
        }
        try {
            Document ping = new Document("ping", "1");
            ret.getDatabase(dbName).runCommand(ping);
        } catch (MongoTimeoutException e) {
            throw new ConfigurationException(
                    String.format("Unable to connect to MongoDB at %s, please check your connection", serverName));
        } finally {
            ret.close();
        }
    }
}
