/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.coremodel;

import org.nuxeo.ecm.core.repository.RepositoryFactory;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.runtime.api.Framework;

/**
 * SQL repository factory.
 * <p>
 * This class is mentioned in the repository extension point defining a given
 * repository. It is constructed by RepositoryManager#getOrRegisterRepository,
 * itself called by the *ManagedConnectionFactory#createRepository of the RA.
 */
public class SQLRepositoryFactory implements RepositoryFactory {

    private String repositoryName;

    @Override
    public void init(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    @Override
    public Object call() {
        SQLRepositoryService sqlRepositoryService = Framework.getLocalService(SQLRepositoryService.class);
        RepositoryDescriptor descriptor = sqlRepositoryService.getRepositoryDescriptor(repositoryName);
        return new SQLRepository(descriptor);
    }

}
