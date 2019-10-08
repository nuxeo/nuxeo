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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.nuxeo.common.utils.FileVersion;
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
        // start before repository and directories
        return ComponentStartOrders.REPOSITORY - 10;
    }

    /**
     * @param databaseName the connection name to retrieve.
     * @return the database configured by {@link MongoDBConnectionConfig} for the input name, or the default one if it
     *         doesn't exist
     */
    @Override
    public MongoDatabase getDatabase(String databaseName) {
        MongoDBConnectionConfig config = getDescriptor(XP_CONNECTION, databaseName);
        MongoClient client = clients.get(databaseName);
        if (client == null) {
            config = getDescriptor(XP_CONNECTION, DEFAULT_CONNECTION_ID);
            client = clients.get(DEFAULT_CONNECTION_ID);
        }
        MongoDatabase db = MongoDBConnectionHelper.getDatabase(client, config.dbname);
        // Stores server version if missing
        MongoDBCountHelper.versions.putIfAbsent(db.getName(), fetchVersion(db));
        return db;
    }

    /**
     * @return all configured databases
     */
    @Override
    public Iterable<MongoDatabase> getDatabases() {
        return () -> clients.entrySet().stream().map(e -> {
            MongoDBConnectionConfig c = getDescriptor(XP_CONNECTION, e.getKey());
            MongoDatabase db = MongoDBConnectionHelper.getDatabase(e.getValue(), c.dbname);
            // Stores server version if missing
            MongoDBCountHelper.versions.putIfAbsent(db.getName(), fetchVersion(db));
            return db;
        }).iterator();
    }

    /**
     * @return the simplified version of the database server. This string will take the format <major>.<minor>.<patch>
     *         in the case of a release, but development builds may contain additional information.
     * @since 11.1
     */
    protected String fetchVersion(MongoDatabase db) {
        Document result = db.runCommand(new Document("buildInfo", 1));
        return (String) result.get("version");
    }

    /**
     * This helper is intended only for Nuxeo 10.X to ensure compatibility with MongoDB [3.4,3.8[
     *
     * @since 11.1
     */
    public static class MongoDBCountHelper {

        private MongoDBCountHelper() {
            // Empty
        }

        protected static final Map<String, String> versions = new HashMap<>();

        protected static final FileVersion FLOOR_VERSION = new FileVersion("3.8");

        /**
         * Counts all documents on a specific repository.
         *
         * @param collection the collection to count in
         * @return the number of documents found
         * @since 11.1
         */
        @SuppressWarnings("deprecation")
        public static long countDocuments(MongoCollection<Document> collection) {
            return new FileVersion(versions.get(collection.getNamespace().getDatabaseName())).compareTo(
                    FLOOR_VERSION) >= 0 ? collection.countDocuments() : collection.count(); // NOSONAR
        }

        /**
         * Counts all documents that comply to a given filter on a specific repository.
         *
         * @param collection the collection to count in
         * @param filter the filter to comply to
         * @return the number of documents found
         * @since 11.1
         */
        @SuppressWarnings("deprecation")
        public static long countDocuments(MongoCollection<Document> collection, Bson filter) {
            return new FileVersion(versions.get(collection.getNamespace().getDatabaseName())).compareTo(
                    FLOOR_VERSION) >= 0 ? collection.countDocuments(filter) : collection.count(filter); // NOSONAR
        }

        /**
         * Counts documents depending on given count options that comply to a given filter on a specific repository.
         *
         * @param collection the collection to count in
         * @param filter the filter to comply to
         * @param options the count options to consider
         * @return the number of documents found
         * @since 11.1
         */
        @SuppressWarnings("deprecation")
        public static long countDocuments(MongoCollection<Document> collection, Bson filter, CountOptions options) {
            return new FileVersion(versions.get(collection.getNamespace().getDatabaseName())).compareTo(
                    FLOOR_VERSION) >= 0 ? collection.countDocuments(filter, options)
                            : collection.count(filter, options); // NOSONAR
        }
    }
}
