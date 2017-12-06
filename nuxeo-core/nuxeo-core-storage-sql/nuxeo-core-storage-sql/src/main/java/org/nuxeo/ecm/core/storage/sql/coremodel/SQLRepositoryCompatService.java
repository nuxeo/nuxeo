/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.sql.coremodel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Compatibility component to register old-style VCS repository extension points.
 *
 * @since 5.9.3
 */
public class SQLRepositoryCompatService extends DefaultComponent {

    private static final Log log = LogFactory.getLog(SQLRepositoryCompatService.class);

    private static final String XP_REPOSITORY = "repository";

    @Override
    public void registerContribution(Object contrib, String xpoint, ComponentInstance contributor) {
        if (XP_REPOSITORY.equals(xpoint)) {
            addContribution((RepositoryDescriptor) contrib);
        } else {
            throw new RuntimeException("Unknown extension point: " + xpoint);
        }
    }

    @Override
    public void unregisterContribution(Object contrib, String xpoint, ComponentInstance contributor) {
        if (XP_REPOSITORY.equals(xpoint)) {
            removeContribution((RepositoryDescriptor) contrib);
        } else {
            throw new RuntimeException("Unknown extension point: " + xpoint);
        }
    }

    protected void addContribution(RepositoryDescriptor cdesc) {
        log.warn("Using old-style extension point" + " org.nuxeo.ecm.core.repository.RepositoryService"
                + " for repository \"" + cdesc.name
                + "\", use org.nuxeo.ecm.core.storage.sql.RepositoryService instead");
        RepositoryDescriptor descriptor = getRepositoryDescriptor(cdesc);
        SQLRepositoryService sqlRepositoryService = Framework.getService(SQLRepositoryService.class);
        sqlRepositoryService.addContribution(descriptor);
    }

    protected void removeContribution(RepositoryDescriptor cdesc) {
        RepositoryDescriptor descriptor = getRepositoryDescriptor(cdesc);
        SQLRepositoryService sqlRepositoryService = Framework.getService(SQLRepositoryService.class);
        sqlRepositoryService.removeContribution(descriptor);
    }

    protected RepositoryDescriptor getRepositoryDescriptor(RepositoryDescriptor cdesc) {
        RepositoryDescriptor descriptor = cdesc.repositoryDescriptor;
        if (descriptor == null) {
            // old-style extension point with new-style descriptor
            // without nested repository
            descriptor = cdesc;
        } else {
            if (descriptor.name == null) {
                descriptor.name = cdesc.name;
            }
        }
        return descriptor;
    }

}
