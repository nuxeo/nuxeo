package org.nuxeo.webengine.management.adapters;

/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *    Stephane Lacoin (Nuxeo EP Software Engineer)
 */

import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ObjectNameFactory;
import org.nuxeo.runtime.management.ResourceDescriptor;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 * 
 */
public class RepositorySessionAccountingMBeanAdapter implements RepositorySessionAccountingMBean {

    public RepositorySessionAccountingMBeanAdapter(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public RepositorySessionAccountingMBeanAdapter(ResourceDescriptor descriptor) {
        this.repositoryName = ObjectNameFactory.getObjectName(
                descriptor.getName()).getKeyProperty("repository");
    }

    protected final String repositoryName;
    
    public String getRepositoryName() {
        return repositoryName;
    }

    protected Repository guardedRepository() {
        RepositoryService service = null;
        try {
            service = (RepositoryService) Framework.getRuntime().getComponent(
                    RepositoryService.NAME);
        } catch (Exception cause) {
            throw new ClientRuntimeException("Cannot get repository service",
                    cause);
        }
        if (service == null) {
            throw new ClientRuntimeException("Cannot get repository service");
        }
        Repository repository = null;
        try {
            repository = service.getRepositoryManager().getRepository(
                    repositoryName);
        } catch (Exception cause) {
            throw new ClientRuntimeException("Cannot get repository "
                    + repositoryName, cause);
        }
        if (repository == null) {
            throw new ClientRuntimeException("Cannot get repository"
                    + repositoryName);
        }
        return repository;
    }

    public Integer getActiveSessionsCount() {
        return guardedRepository().getActiveSessionsCount();
    }

    public Integer getClosedSessionsCount() {
        return guardedRepository().getClosedSessionsCount();
    }

    public Integer getStartedSessionsCount() {
        return guardedRepository().getStartedSessionsCount();
    }

}
