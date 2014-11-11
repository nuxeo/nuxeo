/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.repository.RepositoryDescriptor;
import org.nuxeo.ecm.core.repository.RepositoryFactory;

/**
 * SQL repository factory.
 * <p>
 * This class is mentioned in the repository extension point defining a given
 * repository. It is constructed by RepositoryManager#getOrRegisterRepository,
 * itself called by the *ManagedConnectionFactory#createRepository of the RA.
 *
 * @author Florent Guillaume
 */
public class SQLRepositoryFactory implements RepositoryFactory {

    @Override
    public Repository createRepository(RepositoryDescriptor descriptor)
            throws Exception {
        return new SQLRepository(descriptor);
    }

}
