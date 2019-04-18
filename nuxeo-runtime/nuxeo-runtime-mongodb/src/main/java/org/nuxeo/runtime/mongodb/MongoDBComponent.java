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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentStartOrders;
import org.nuxeo.runtime.model.DefaultComponent;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

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

    private final Map<String, MongoClient> clients = new ConcurrentHashMap<>();

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
     * @param id the connection id to retrieve.
     * @return the database configured by {@link MongoDBConnectionConfig} for the input id, or the default one if it
     *         doesn't exist
     */
    @SuppressWarnings("resource") // client closed by stop()
    @Override
    public MongoDatabase getDatabase(String id) {
        MongoDBConnectionConfig config = getDescriptor(XP_CONNECTION, id);
        MongoClient client = clients.get(id);
        if (client == null) {
            config = getDescriptor(XP_CONNECTION, DEFAULT_CONNECTION_ID);
            client = clients.get(DEFAULT_CONNECTION_ID);
        }
        return MongoDBConnectionHelper.getDatabase(client, config.dbname);
    }

    /**
     * @return all configured databases
     */
    @Override
    public Iterable<MongoDatabase> getDatabases() {
        return () -> clients.entrySet().stream().map(e -> {
            MongoDBConnectionConfig c = getDescriptor(XP_CONNECTION, e.getKey());
            return MongoDBConnectionHelper.getDatabase(e.getValue(), c.dbname);
        }).iterator();
    }

}
