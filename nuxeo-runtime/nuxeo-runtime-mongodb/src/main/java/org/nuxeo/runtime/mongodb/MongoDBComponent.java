/*
 * (C) Copyright 2017-2018 Nuxeo (http://nuxeo.com/) and others.
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

import static org.nuxeo.runtime.mongodb.MongoDBComponent.MongoDBCountHelper.recordVersion;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.nuxeo.common.utils.FileVersion;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentStartOrders;
import org.nuxeo.runtime.model.DefaultComponent;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CountOptions;

/**
 * Component used to get a database connection to MongoDB. Don't expose {@link MongoClient} directly, because it's this
 * component which is responsible for creating and closing it.
 *
 * @since 9.1
 */
public class MongoDBComponent extends DefaultComponent implements MongoDBConnectionService {

    private static final Logger log = LogManager.getLogger(MongoDBComponent.class);

    /**
     * @since 10.3
     */
    public static final String COMPONENT_NAME = "org.nuxeo.runtime.mongodb.MongoDBComponent";

    private static final String XP_CONNECTION = "connection";

    private static final String DEFAULT_CONNECTION_ID = "default";

    protected final Map<String, MongoClient> clients = new ConcurrentHashMap<>();

    @Override
    public void start(ComponentContext context) {
        super.start(context);
        Collection<MongoDBConnectionConfig> confs = getDescriptors(XP_CONNECTION);
        confs.forEach(c -> {
            log.debug("Initializing MongoClient with id={}", c::getId);
            clients.put(c.getId(), MongoDBConnectionHelper.newMongoClient(c));
        });
    }

    @Override
    @SuppressWarnings("Java8MapForEach")
    public void stop(ComponentContext context) throws InterruptedException {
        super.stop(context);
        // don't remove entrySet otherwise java will try to load mongo client classes even in a non mongo setup
        clients.entrySet().forEach(e -> {
            log.debug("Closing MongoClient with id={}", e::getKey);
            e.getValue().close();
        });
        clients.clear();
    }

    @Override
    public int getApplicationStartedOrder() {
        return ComponentStartOrders.MONGODB;
    }

    @SuppressWarnings("resource") // client closed by stop()
    @Override
    public MongoClient getClient(String id) {
        MongoClient client = clients.get(id);
        if (client == null) {
            client = clients.get(DEFAULT_CONNECTION_ID);
        }
        return client;
    }

    @Override
    public MongoDBConnectionConfig getConfig(String id) {
        MongoDBConnectionConfig config = getDescriptor(XP_CONNECTION, id);
        if (config == null) {
            config = getDescriptor(XP_CONNECTION, DEFAULT_CONNECTION_ID);
        }
        return config;
    }

    /**
     * @param id the connection id to retrieve.
     * @return the database configured by {@link MongoDBConnectionConfig} for the input id, or the default one if it
     *         doesn't exist
     */
    @Override
    public MongoDatabase getDatabase(String id) {
        MongoDBConnectionConfig config = getDescriptor(XP_CONNECTION, id);
        MongoClient client = clients.get(id);
        if (client == null) {
            config = getDescriptor(XP_CONNECTION, DEFAULT_CONNECTION_ID);
            client = clients.get(DEFAULT_CONNECTION_ID);
        }
        return getDatabase(client, config);
    }

    /**
     * @return all configured databases
     */
    @Override
    public Iterable<MongoDatabase> getDatabases() {
        return () -> clients.entrySet().stream().map(e -> {
            MongoDBConnectionConfig c = getDescriptor(XP_CONNECTION, e.getKey());
            return getDatabase(e.getValue(), c);
        }).iterator();
    }

    /**
     * Records the version of the mongo server containing the database and returns the database.
     *
     * @param client the mongo client
     * @param config the mongo connection config descriptor
     * @return the mongo database after recording the mongo server version.
     * @since 10.10-HF16
     */
    protected MongoDatabase getDatabase(MongoClient client, MongoDBConnectionConfig config) {
        MongoDatabase db = MongoDBConnectionHelper.getDatabase(client, config.dbname);
        recordVersion(db, config);
        return db;
    }

    /**
     * This helper is intended only for Nuxeo 10.X to ensure compatibility with MongoDB [3.4,3.8[
     *
     * @since 10.10-HF16
     */
    public static class MongoDBCountHelper {

        private MongoDBCountHelper() {
            // Empty
        }

        protected static final Map<String, FileVersion> versions = new ConcurrentHashMap<>();

        protected static final FileVersion FLOOR_VERSION = new FileVersion("3.8");

        /**
         * Counts all documents on a specific repository.
         *
         * @param database the database whose version we want to check
         * @param collection the collection to count in
         * @return the number of documents found
         * @since 10.10-HF16
         */
        @SuppressWarnings("deprecation")
        public static long countDocuments(String database, MongoCollection<Document> collection) {
            return isRecentMongo(database) ? collection.countDocuments() : collection.count(); // NOSONAR
        }

        /**
         * Counts all documents that comply to a given filter on a specific repository.
         *
         *@param database the database whose version we want to check
         * @param collection the collection to count in
         * @param filter the filter to comply to
         * @return the number of documents found
         * @since 10.10-HF16
         */
        @SuppressWarnings("deprecation")
        public static long countDocuments(String database, MongoCollection<Document> collection, Bson filter) {
            return isRecentMongo(database) ? collection.countDocuments(filter) : collection.count(filter); // NOSONAR
        }

        /**
         * Counts documents depending on given count options that comply to a given filter on a specific repository.
         *
         * @param database the database whose version we want to check
         * @param collection the collection to count in
         * @param filter the filter to comply to
         * @param options the count options to consider
         * @return the number of documents found
         * @since 10.10-HF16
         */
        @SuppressWarnings("deprecation")
        public static long countDocuments(String database, MongoCollection<Document> collection, Bson filter,
                CountOptions options) {
            return isRecentMongo(database) ? collection.countDocuments(filter, options)
                    : collection.count(filter, options); // NOSONAR
        }

        /**
         * Checks if the mongo server is above or equal to the floor version.
         *
         * @param databaseID the key to find the version
         * @return the version of the server which contains the database
         * @since 10.10-HF16
         */
        public static boolean isRecentMongo(String databaseID) {
            return versions.getOrDefault(databaseID, versions.get(DEFAULT_CONNECTION_ID)).compareTo(FLOOR_VERSION) >= 0;
        }

        /**
         * Records the version of the mongo server on which a database is living.
         *
         * @param client the mongo client
         * @param config the mongo connection config
         * @since 10.10-HF16
         */
        public static void recordVersion(MongoDatabase database, MongoDBConnectionConfig config) {
            versions.computeIfAbsent(config.id, dbID -> {
                Document result = database.runCommand(new Document("buildInfo", 1));
                return new FileVersion((String) result.get("version"));
            });
        }
    }
}
