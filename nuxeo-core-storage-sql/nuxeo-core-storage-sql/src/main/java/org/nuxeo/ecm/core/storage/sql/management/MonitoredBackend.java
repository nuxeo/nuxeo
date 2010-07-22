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

    RepositoryBackend wrapped;

    protected MonitoredBackend(RepositoryBackend wrapped) {
       this.wrapped = wrapped;
    }

    @Override
    public Mapper getMapper(Model model, PathResolver pathResolver) throws StorageException {
        return MetricInvocationHandler.newProxy(wrapped.getMapper(model, pathResolver),Mapper.class);
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
    public void initializeModelSetup(ModelSetup modelSetup) throws StorageException {
       wrapped.initializeModelSetup(modelSetup);
    }

    @Override
    public void shutdown() throws StorageException {
       wrapped.shutdown();
    }

}
