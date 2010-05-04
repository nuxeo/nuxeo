/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.net;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Mapper;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.ModelSetup;
import org.nuxeo.ecm.core.storage.sql.RepositoryBackend;
import org.nuxeo.ecm.core.storage.sql.RepositoryImpl;

/**
 * Network client backend for a repository.
 */
public class NetBackend implements RepositoryBackend {

    protected RepositoryImpl repository;

    protected HttpClient httpClient;

    protected MultiThreadedHttpConnectionManager connectionManager;

    public void initialize(RepositoryImpl repository) throws StorageException {
        this.repository = repository;
        connectionManager = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams params = connectionManager.getParams();
        params.setDefaultMaxConnectionsPerHost(20);
        params.setMaxTotalConnections(20);
        httpClient = new HttpClient(connectionManager);
    }

    public void initializeModelSetup(ModelSetup modelSetup)
            throws StorageException {
        modelSetup.materializeFulltextSyntheticColumn = false; // TODO-H2
    }

    public void initializeModel(Model model) throws StorageException {
    }

    public Mapper newMapper(Model model) throws StorageException {
        return NetMapper.getMapper(repository, httpClient);
    }

    public void shutdown() {
        connectionManager.shutdown();
    }

}
