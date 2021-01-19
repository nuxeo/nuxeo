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

import static java.util.concurrent.TimeUnit.SECONDS;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.nuxeo.launcher.config.ConfigurationException;
import org.nuxeo.launcher.config.ConfigurationHolder;
import org.nuxeo.launcher.config.backingservices.BackingChecker;
import org.nuxeo.runtime.mongodb.MongoDBConnectionConfig;
import org.nuxeo.runtime.mongodb.MongoDBConnectionHelper;

import com.mongodb.MongoTimeoutException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

/**
 * @since 9.2
 */
public class MongoDBChecker implements BackingChecker {

    private static final Logger log = LogManager.getLogger(MongoDBChecker.class);

    public static final String TEMPLATE_NAME = "mongodb";

    public static final String CONFIG_NAME = "mongodb-connection-config.xml";

    /** @since 9.3 */
    public static final String PARAM_MONGODB_CHECK_TIMEOUT = "nuxeo.mongodb.check.timeout";

    /** @since 9.3 */
    public static final int DEFAULT_CHECK_TIMEOUT_IN_SECONDS = 5;

    @Override
    public boolean accepts(ConfigurationHolder configHolder) {
        return configHolder.getIncludedTemplateNames().contains(TEMPLATE_NAME);
    }

    @Override
    public void check(ConfigurationHolder configHolder) throws ConfigurationException {
        MongoDBConnectionConfig config = getDescriptor(configHolder, CONFIG_NAME, MongoDBConnectionConfig.class);
        try (MongoClient mongoClient = MongoDBConnectionHelper.newMongoClient(config,
                builder -> builder.applicationName("Nuxeo DB Check")
                                  .applyToClusterSettings(s -> s.serverSelectionTimeout(
                                          getCheckTimeoutInSeconds(configHolder), SECONDS)))) {
            MongoDatabase database = mongoClient.getDatabase(config.dbname);
            Document ping = new Document("ping", "1");
            database.runCommand(ping);
        } catch (MongoTimeoutException e) {
            throw new ConfigurationException(
                    String.format("Unable to connect to MongoDB at: %s, please check your connection", config.server),
                    e);
        }
    }

    /**
     * Returns the value of the check timeout parameter in seconds. If value is not parseable or not set, then use the
     * default value.
     *
     * @return the timeout check in seconds.
     * @since 9.3
     */
    private int getCheckTimeoutInSeconds(ConfigurationHolder configHolder) {
        try {
            return configHolder.getPropertyAsInteger(PARAM_MONGODB_CHECK_TIMEOUT, DEFAULT_CHECK_TIMEOUT_IN_SECONDS);
        } catch (NumberFormatException e) {
            log.warn("Invalid format for: {} parameter, using default value instead", PARAM_MONGODB_CHECK_TIMEOUT, e);
            return DEFAULT_CHECK_TIMEOUT_IN_SECONDS;
        }
    }
}
