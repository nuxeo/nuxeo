/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephane Lacoin
 */

package org.nuxeo.ecm.core.storage.sql.management;

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
            MapperKind kind) throws StorageException {
        return MetricInvocationHandler.newProxy(
                wrapped.newMapper(model, pathResolver, kind), Mapper.class);
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
