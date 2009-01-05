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
 *     matic
 */

import javax.management.ObjectName;

import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.repository.RepositoryManagerImpl;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.repository.RepositoryManager;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.AbstractResourceFactory;
import org.nuxeo.runtime.management.ObjectNameFactory;
import org.nuxeo.runtime.management.ResourceFactory;
import org.nuxeo.runtime.management.ResourceFactoryDescriptor;

/**
 * @author matic
 * 
 */
public class RepositorySessionMetricMBeanAdapterFactory extends
        AbstractResourceFactory implements ResourceFactory {

    public RepositorySessionMetricMBeanAdapterFactory(
            ResourceFactoryDescriptor descriptor) {
        super(descriptor);
    }

    protected RepositoryManager repositoryManager() {
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
        return service.getRepositoryManager();
    }

    protected Repository repository(RepositoryManager manager, String name) {
        Repository repository = null;
        try {
            repository = manager.getRepository(name);
        } catch (Exception cause) {
            throw new ClientRuntimeException("Cannot get " + name
                    + " repository", cause);
        }
        if (repository == null) {
            throw new ClientRuntimeException("Cannot get " + name
                    + " repository");
        }
        return repository;
    }

    public void registerResources(Callback callback) {
        RepositoryManager manager = repositoryManager();
        String qualifiedName = ObjectNameFactory.getQualifiedName(RepositoryManagerImpl.NAME.getName());

        callback.invokeFor(ObjectNameFactory.getObjectName(qualifiedName,
                "metric"), WholeRepositoriesSessionMetricMBean.class,
                new WholeRepositoriesSessionMetricMBeanAdapter(manager));

        for (String repositoryName : manager.getRepositoryNames()) {
            ObjectName objectName = ObjectNameFactory.getObjectName(
                    qualifiedName + ",repository=" + repositoryName, "metric");
            Object objectInstance = new RepositorySessionMetricMBeanAdapter(
                    repository(manager, repositoryName));
            callback.invokeFor(objectName, RepositorySessionMetricMBean.class,
                    objectInstance);
            ;
        }
        ;
    }
}
