/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.mongodb;

import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.mongodb.MongoDBComponent;
import org.nuxeo.runtime.mongodb.MongoDBConnectionConfig;

/**
 * Service holding the configuration for MongoDB repositories.
 *
 * @since 5.9.4
 */
public class MongoDBRepositoryService extends DefaultComponent {

    public static final String DB_DEFAULT = "nuxeo";

    private static final String XP_REPOSITORY = "repository";

    @Override
    public void registerContribution(Object contrib, String xpoint, ComponentInstance contributor) {
        if (XP_REPOSITORY.equals(xpoint)) {
            MongoDBRepositoryDescriptor desc = (MongoDBRepositoryDescriptor) contrib;
            Framework.getService(DBSRepositoryService.class).addContribution(desc, MongoDBRepositoryFactory.class);
            handleConnectionContribution(desc, (c, d) -> c.registerContribution(d, "connection", contributor));
        }
    }

    @Override
    public void unregisterContribution(Object contrib, String xpoint, ComponentInstance contributor) {
        if (XP_REPOSITORY.equals(xpoint)) {
            MongoDBRepositoryDescriptor desc = (MongoDBRepositoryDescriptor) contrib;
            Framework.getService(DBSRepositoryService.class).removeContribution(desc, MongoDBRepositoryFactory.class);
            handleConnectionContribution(desc, (c, d) -> c.unregisterContribution(d, "connection", contributor));
        }
    }

    /**
     * Backward compatibility for {@link MongoDBRepositoryDescriptor#server descriptor.server} and
     * {@link MongoDBRepositoryDescriptor#dbname descriptor.dbname}
     *
     * @since 9.3
     * @deprecated since 9.3
     */
    @Deprecated
    protected void handleConnectionContribution(MongoDBRepositoryDescriptor descriptor,
            BiConsumer<DefaultComponent, MongoDBConnectionConfig> consumer) {
        if (StringUtils.isNotBlank(descriptor.server)) {
            String id = "repository/" + descriptor.name;
            String server = descriptor.server;
            String dbName = StringUtils.defaultIfBlank(descriptor.dbname, DB_DEFAULT);
            MongoDBConnectionConfig connection = new MongoDBConnectionConfig();
            connection.server = server;
            connection.dbname = dbName;
            connection.id = id;

            DefaultComponent component = (DefaultComponent) Framework.getRuntime()
                                                                     .getComponent(MongoDBComponent.COMPONENT_NAME);
            consumer.accept(component, connection);
        }
    }

}
