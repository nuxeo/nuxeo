/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stephane Lacoin
 */

package org.nuxeo.ecm.core.storage.sql.management;

import org.nuxeo.ecm.core.storage.Credentials;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Mapper;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.ModelSetup;
import org.nuxeo.ecm.core.storage.sql.RepositoryBackend;
import org.nuxeo.ecm.core.storage.sql.RepositoryImpl;
import org.nuxeo.ecm.core.storage.sql.Session.PathResolver;
import org.nuxeo.runtime.management.metrics.MetricInvocationHandler;

public abstract class MonitoredBackend implements RepositoryBackend {

    final RepositoryBackend wrapped;

    protected MonitoredBackend(RepositoryBackend wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public Mapper newMapper(Model model, PathResolver pathResolver,
            Credentials credentials, boolean create) throws StorageException {
        return MetricInvocationHandler.newProxy(
                wrapped.newMapper(model, pathResolver, null, create), Mapper.class);
    }

    @Override
    public void initialize(RepositoryImpl repository) throws StorageException {
        wrapped.initialize(repository);
    }

    @Override
    public void initializeModel(Model model) throws StorageException {
        wrapped.initializeModel(model);
    }

    @Override
    public void initializeModelSetup(ModelSetup modelSetup)
            throws StorageException {
        wrapped.initializeModelSetup(modelSetup);
    }

    @Override
    public void shutdown() throws StorageException {
        wrapped.shutdown();
    }

}
