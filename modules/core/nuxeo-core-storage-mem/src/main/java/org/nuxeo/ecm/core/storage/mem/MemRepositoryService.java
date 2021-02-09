/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.mem;

import org.nuxeo.ecm.core.storage.dbs.AbstractDBSRepositoryDescriptorRegistry;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryRegistry;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Service holding the configuration for Memory repositories.
 * <p>
 * Since 11.5, all logic is led by associated registry.
 *
 * @since 8.1
 */
public class MemRepositoryService extends DefaultComponent {

    protected static final String COMPONENT_NAME = "org.nuxeo.ecm.core.storage.mem.MemRepositoryService";

    protected static final String XP = "repository";

    /**
     * Registry for {@link MemRepositoryDescriptor}, forwarding to {@link DBSRepositoryRegistry}.
     * <p>
     * Also handles custom merge.
     *
     * @since 11.5
     */
    public static final class Registry extends AbstractDBSRepositoryDescriptorRegistry {

        public Registry() {
            super(COMPONENT_NAME, XP, MemRepositoryFactory.class);
        }

    }

}
