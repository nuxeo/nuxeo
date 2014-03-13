/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 * Compatibility component to register old-style VCS repository extension
 * points.
 *
 * @since 5.9.3
 */
public class SQLRepositoryCompatService extends DefaultComponent {

    private static final Log log = LogFactory.getLog(SQLRepositoryCompatService.class);

    private static final String XP_REPOSITORY = "repository";

    @Override
    public void registerContribution(Object contrib, String xpoint,
            ComponentInstance contributor) {
        if (XP_REPOSITORY.equals(xpoint)) {
            addContribution((RepositoryDescriptor) contrib);
        } else {
            throw new RuntimeException("Unknown extension point: " + xpoint);
        }
    }

    @Override
    public void unregisterContribution(Object contrib, String xpoint,
            ComponentInstance contributor) throws Exception {
        if (XP_REPOSITORY.equals(xpoint)) {
            removeContribution((RepositoryDescriptor) contrib);
        } else {
            throw new RuntimeException("Unknown extension point: " + xpoint);
        }
    }

    protected void addContribution(RepositoryDescriptor cdesc) {
        log.warn("Using old-style extension point"
                + " org.nuxeo.ecm.core.repository.RepositoryService"
                + " for repository \""
                + cdesc.name
                + "\", use org.nuxeo.ecm.core.storage.sql.RepositoryService instead");
        RepositoryDescriptor descriptor = getRepositoryDescriptor(cdesc);
        SQLRepositoryService sqlRepositoryService = Framework.getLocalService(SQLRepositoryService.class);
        sqlRepositoryService.addContribution(descriptor);
    }

    protected void removeContribution(RepositoryDescriptor cdesc) {
        RepositoryDescriptor descriptor = getRepositoryDescriptor(cdesc);
        SQLRepositoryService sqlRepositoryService = Framework.getLocalService(SQLRepositoryService.class);
        sqlRepositoryService.removeContribution(descriptor);
    }

    protected RepositoryDescriptor getRepositoryDescriptor(
            RepositoryDescriptor cdesc) {
        RepositoryDescriptor descriptor = cdesc.repositoryDescriptor;
        if (descriptor == null) {
            // old-style extension point with new-style descriptor
            // without nested repository
            descriptor = cdesc;
        } else {
            descriptor.setRepositoryFactoryClass(cdesc.getRepositoryFactoryClass());
            if (descriptor.name == null) {
                descriptor.name = cdesc.name;
            }
        }
        return descriptor;
    }

}
