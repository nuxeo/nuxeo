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

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

/**
 * Service used to get a database connection to MongoDB.
 *
 * @since 9.1
 */
public interface MongoDBConnectionService {

    /**
     * Gets the MongoDB client for the given id.
     *
     * @param id the connection id
     * @return the client configured by {@link MongoDBConnectionConfig} for the input id, or the default one if it
     *         doesn't exist
     * @since 11.1
     */
    MongoClient getClient(String id);

    /**
     * Gets the MongoDB configuration for the given id.
     *
     * @since 11.1
     */
    MongoDBConnectionConfig getConfig(String id);

    /**
     * Gets the MongoDB database name for the given id.
     *
     * @param id the connection id
     * @return the database name configured by {@link MongoDBConnectionConfig} for the input id, or the default one if
     *         it doesn't exist
     * @since 11.1
     */
    String getDatabaseName(String id);

    /**
     * @param id the connection id
     * @return the database configured by {@link MongoDBConnectionConfig} for the input id, or the default one if it
     *         doesn't exist
     */
    MongoDatabase getDatabase(String id);

    /**
     * @return all configured databases
     * @deprecated since 11.1, unused
     */
    @Deprecated
    Iterable<MongoDatabase> getDatabases();

}
