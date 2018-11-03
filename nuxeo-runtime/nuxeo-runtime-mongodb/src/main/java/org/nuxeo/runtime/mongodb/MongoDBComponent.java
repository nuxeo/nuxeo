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

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentStartOrders;
import org.nuxeo.runtime.model.ContributionFragmentRegistry.FragmentList;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

/**
 * Component used to get a database connection to MongoDB. Don't expose {@link MongoClient} directly, because it's this
 * component which is responsible for creating and closing it.
 *
 * @since 9.1
 */
public class MongoDBComponent extends DefaultComponent implements MongoDBConnectionService {

    private static final Log log = LogFactory.getLog(MongoDBComponent.class);

    public static final String NAME = "org.nuxeo.runtime.mongodb.MongoDBComponent";

    private static final String EP_CONNECTION = "connection";

    private static final String DEFAULT_CONNECTION_ID = "default";

    private final MongoDBConnectionConfigRegistry registry = new MongoDBConnectionConfigRegistry();

    private final Map<String, MongoClient> clients = new ConcurrentHashMap<>();

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        switch (extensionPoint) {
        case EP_CONNECTION:
            registry.addContribution((MongoDBConnectionConfig) contribution);
            log.info(
                    "Registering connection configuration: " + contribution + ", loaded from " + contributor.getName());
            break;
        default:
            throw new IllegalStateException("Invalid EP: " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        switch (extensionPoint) {
        case EP_CONNECTION:
            MongoDBConnectionConfig config = (MongoDBConnectionConfig) contribution;
            log.info("Unregistering connection configuration: " + config);
            clients.remove(config.getId()).close();
            registry.removeContribution(config);
            break;
        default:
            throw new IllegalStateException("Invalid EP: " + extensionPoint);
        }
    }

    @Override
    public void start(ComponentContext context) {
        log.info("Activate MongoDB component");
        for (FragmentList<MongoDBConnectionConfig> fragment : registry.getFragments()) {
            MongoDBConnectionConfig conf = fragment.object;
            log.debug("Initializing MongoClient with id=" + conf.getId());
            @SuppressWarnings("resource")
            MongoClient client = MongoDBConnectionHelper.newMongoClient(conf);
            clients.put(conf.getId(), client);
        }
    }

    @Override
    public void stop(ComponentContext context) {
        Iterator<Entry<String, MongoClient>> it = clients.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, MongoClient> entry = it.next();
            log.debug("Closing MongoClient with id=" + entry.getKey());
            MongoClient client = entry.getValue();
            client.close();
            it.remove();
        }
    }

    @Override
    public int getApplicationStartedOrder() {
        // start before repository
        return ComponentStartOrders.REPOSITORY - 1;
    }

    /**
     * @param id the connection id to retrieve.
     * @return the database configured by {@link MongoDBConnectionConfig} for the input id, or the default one if it
     *         doesn't exist
     */
    @Override
    public MongoDatabase getDatabase(String id) {
        MongoDBConnectionConfig config = registry.getCurrentContribution(id);
        MongoClient client = clients.get(id);
        if (client == null) {
            config = registry.getCurrentContribution(DEFAULT_CONNECTION_ID);
            client = clients.get(DEFAULT_CONNECTION_ID);
        }
        return MongoDBConnectionHelper.getDatabase(client, config.getDbname());
    }

    /**
     * @return all configured databases
     */
    @Override
    public Iterable<MongoDatabase> getDatabases() {
        return () -> clients.entrySet()
                            .stream()
                            .map(e -> MongoDBConnectionHelper.getDatabase(e.getValue(),
                                    registry.getCurrentContribution(e.getKey()).getDbname()))
                            .iterator();
    }

    protected static class MongoDBConnectionConfigRegistry extends SimpleContributionRegistry<MongoDBConnectionConfig> {

        @Override
        public String getContributionId(MongoDBConnectionConfig contrib) {
            return contrib.getId();
        }

        @Override
        public MongoDBConnectionConfig getCurrentContribution(String id) {
            return super.getCurrentContribution(id);
        }

    }

}
