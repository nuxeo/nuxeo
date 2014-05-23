/*
 * Copyright (c) 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.mongodb;

import org.nuxeo.ecm.core.repository.RepositoryFactory;
import org.nuxeo.runtime.api.Framework;

/**
 * MongoDB implementation of a {@link RepositoryFactory}, creating a
 * {@link MongoDBRepository}.
 *
 * @since 5.9.4
 */
public class MongoDBRepositoryFactory implements RepositoryFactory {

    protected String repositoryName;

    @Override
    public void init(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    @Override
    public Object call() {
        MongoDBRepositoryService repositoryService = Framework.getLocalService(MongoDBRepositoryService.class);
        MongoDBRepositoryDescriptor descriptor = repositoryService.getRepositoryDescriptor(repositoryName);
        if (descriptor == null) {
            throw new IllegalStateException("No descriptor registered for: "
                    + repositoryName);
        }
        return new MongoDBRepository(descriptor);
    }

}
