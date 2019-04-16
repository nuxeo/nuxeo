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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.launcher.config.ConfigurationException;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.launcher.config.backingservices.BackingChecker;
import org.nuxeo.runtime.mongodb.MongoDBConnectionConfig;
import org.nuxeo.runtime.mongodb.MongoDBConnectionHelper;

import com.mongodb.MongoClient;
import com.mongodb.MongoTimeoutException;
import com.mongodb.client.MongoDatabase;

public class MongoDBChecker implements BackingChecker {

    private static final Log log = LogFactory.getLog(MongoDBChecker.class);

    public static final String TEMPLATE_NAME = "mongodb";

    public static final String CONFIG_NAME = "mongodb-connection-config.xml";

    /**
     * @since 9.3
     */
    public static final String PARAM_MONGODB_CHECK_TIMEOUT = "nuxeo.mongodb.check.timeout";

    /**
     * @since 9.3
     */
    public static final int DEFAULT_CHECK_TIMEOUT_IN_SECONDS = 5;


    @Override
    public boolean accepts(ConfigurationGenerator cg) {
        return cg.getTemplateList().contains(TEMPLATE_NAME);
    }

    @Override
    public void check(ConfigurationGenerator cg) throws ConfigurationException {
        File configFile = new File(cg.getConfigDir(), CONFIG_NAME);
        if (!configFile.exists()) {
            log.warn("Unable to find config file " + CONFIG_NAME);
        } else {
            MongoDBConnectionConfig config = getDescriptor(configFile, MongoDBConnectionConfig.class);
            try (MongoClient mongoClient = MongoDBConnectionHelper.newMongoClient(config,
                    builder -> builder.serverSelectionTimeout(
                            (int) TimeUnit.SECONDS.toMillis(getCheckTimeoutInSeconds(cg)))
                                      .description("Nuxeo DB Check"))) {
                MongoDatabase database = mongoClient.getDatabase(config.dbname);
                Document ping = new Document("ping", "1");
                database.runCommand(ping);
            } catch (MongoTimeoutException e) {
                throw new ConfigurationException(String.format(
                        "Unable to connect to MongoDB at %s, please check your connection", config.server));
            }
        }
    }

    /**
     * Creates a descriptor instance for the specified file and descriptor class.
     *
     * @since 11.1
     */
    public <T> T getDescriptor(File file, Class<T> klass) throws ConfigurationException {
        XMap xmap = new XMap();
        xmap.register(klass);
        try (InputStream inStream = new FileInputStream(file)) {
            return (T) xmap.load(inStream);
        } catch (IOException e) {
            throw new ConfigurationException("Unable to load the configuration for " + klass.getSimpleName(), e);
        }
    }

    /**
     * Returns the value of the check timeout parameter in seconds.
     * If value is not parseable or not set, then use the default value.
     * @return the timeout check in seconds.
     * @since 9.3
     */
    private int getCheckTimeoutInSeconds(ConfigurationGenerator cg) {
        int checkTimeout = DEFAULT_CHECK_TIMEOUT_IN_SECONDS;
        try {
            checkTimeout = Integer.parseInt(cg.getUserConfig().getProperty(PARAM_MONGODB_CHECK_TIMEOUT, String.valueOf(DEFAULT_CHECK_TIMEOUT_IN_SECONDS)));
        } catch (NumberFormatException e) {
            log.warn(String.format("Invalid format for %s parameter, using default value instead", PARAM_MONGODB_CHECK_TIMEOUT), e);
        }
        return checkTimeout;
    }
}
