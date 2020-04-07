/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 */

package org.nuxeo.ecm.core.storage.sql.management;

import org.nuxeo.ecm.core.storage.sql.VCSClusterInvalidator;
import org.nuxeo.ecm.core.storage.sql.Mapper;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.RepositoryBackend;
import org.nuxeo.ecm.core.storage.sql.RepositoryImpl;
import org.nuxeo.ecm.core.storage.sql.Session.PathResolver;
import org.nuxeo.ecm.core.storage.sql.jdbc.SQLInfo;
import org.nuxeo.runtime.management.metrics.MetricInvocationHandler;

public abstract class MonitoredBackend implements RepositoryBackend {

    final RepositoryBackend wrapped;

    protected MonitoredBackend(RepositoryBackend wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public Mapper newMapper(PathResolver pathResolver, boolean useInvalidations) {
        return MetricInvocationHandler.newProxy(wrapped.newMapper(pathResolver, useInvalidations), Mapper.class);
    }

    @Override
    public Model initialize(RepositoryImpl repository) {
        return wrapped.initialize(repository);
    }

    @Override
    public void setClusterInvalidator(VCSClusterInvalidator clusterInvalidator) {
        wrapped.setClusterInvalidator(clusterInvalidator);
    }

    @Override
    public SQLInfo getSQLInfo() {
        return wrapped.getSQLInfo();
    }

    @Override
    public void shutdown() {
        wrapped.shutdown();
    }

}
