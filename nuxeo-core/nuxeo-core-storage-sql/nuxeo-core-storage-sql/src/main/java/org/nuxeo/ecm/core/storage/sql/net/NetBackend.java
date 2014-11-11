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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Mapper;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.ModelSetup;
import org.nuxeo.ecm.core.storage.sql.RepositoryBackend;
import org.nuxeo.ecm.core.storage.sql.RepositoryImpl;
import org.nuxeo.ecm.core.storage.sql.Session.PathResolver;

/**
 * Network client backend for a repository.
 */
public class NetBackend implements RepositoryBackend {

    private static final Log log = LogFactory.getLog(NetBackend.class);

    protected RepositoryImpl repository;

    public void initialize(RepositoryImpl repository) throws StorageException {
        this.repository = repository;
    }

    public void initializeModelSetup(ModelSetup modelSetup)
            throws StorageException {
        modelSetup.materializeFulltextSyntheticColumn = false; // TODO-H2
    }

    public void initializeModel(Model model) throws StorageException {
    }

    public Mapper newMapper(Model model, PathResolver pathResolver)
            throws StorageException {
        try {
            return MapperClient.getMapper(repository);
        } catch (StorageException e) {
            String url = MapperClient.getUrl(repository.getRepositoryDescriptor());
            log.error("Failed to connect to server: " + url, e);
            throw e;
        }
    }

    public void shutdown() {
    }

}
