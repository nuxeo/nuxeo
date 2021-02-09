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

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.ecm.core.storage.dbs.AbstractDBSRepositoryDescriptorRegistry;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryRegistry;
import org.nuxeo.runtime.RuntimeMessage;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.RuntimeMessage.Source;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.logging.DeprecationLogger;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.mongodb.MongoDBComponent;
import org.w3c.dom.Element;

/**
 * Service holding the configuration for MongoDB repositories.
 * <p>
 * Since 11.5, all logic is led by associated registry {@link Registry}.
 *
 * @since 5.9.4
 */
public class MongoDBRepositoryService extends DefaultComponent {

    protected static final String COMPONENT_NAME = "org.nuxeo.ecm.core.storage.mongodb.MongoDBRepositoryService";

    protected static final String XP = "repository";

    /**
     * Registry for {@link MongoDBRepositoryDescriptor}, forwarding to {@link DBSRepositoryRegistry}.
     * <p>
     * Also handles custom merge.
     *
     * @since 11.5
     */
    public static final class Registry extends AbstractDBSRepositoryDescriptorRegistry {

        public Registry() {
            super(COMPONENT_NAME, XP, MongoDBRepositoryFactory.class);
        }

        @Override
        @SuppressWarnings({ "unchecked", "deprecation" })
        protected <T> T doRegister(Context ctx, XAnnotatedObject xObject, Element element, String extensionId) {
            MongoDBRepositoryDescriptor contrib = super.doRegister(ctx, xObject, element, extensionId);
            if (StringUtils.isNotBlank(contrib.server) || StringUtils.isNotBlank(contrib.dbname)) {
                String msg = String.format(
                        "MongoDB repository contribution with name '%s' holds 'server' or 'dbname' elements that"
                                + " should be contributed to extension point '%s--%s' since 9.3",
                        contrib.name, MongoDBComponent.COMPONENT_NAME, "connection");
                DeprecationLogger.log(msg, "9.3");
                Framework.getRuntime()
                         .getMessageHandler()
                         .addMessage(new RuntimeMessage(Level.ERROR, msg, Source.EXTENSION, extensionId));
            }
            return (T) contrib;
        }

    }

}
