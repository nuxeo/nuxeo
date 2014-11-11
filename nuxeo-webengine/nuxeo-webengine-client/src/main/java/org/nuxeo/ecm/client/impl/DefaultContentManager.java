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
package org.nuxeo.ecm.client.impl;

import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.client.Connector;
import org.nuxeo.ecm.client.CannotConnectToServerException;
import org.nuxeo.ecm.client.ContentHandlerRegistry;
import org.nuxeo.ecm.client.ContentManager;
import org.nuxeo.ecm.client.NoSuchRepositoryException;
import org.nuxeo.ecm.client.Repository;
import org.nuxeo.ecm.client.RepositoryService;
import org.nuxeo.ecm.client.commands.RepositoriesCommand;

/**
 * @author matic
 *
 */
public class DefaultContentManager implements ContentManager, RepositoryService {

    protected URL baseURL;

    protected Connector connector;


    protected final ContentHandlerRegistry contentHandlerRegistry = new ContentHandlerRegistry();

    protected final Log log = LogFactory.getLog(DefaultContentManager.class);


    public void init(URL baseURL, Class<? extends Connector> connectorClass) throws CannotInstantiateConnectorException {
        this.baseURL = baseURL;
        try {
            connector = connectorClass.newInstance();
        } catch (Exception e) {
            throw new CannotInstantiateConnectorException(connectorClass, e);
        }
        connector.init(this);
    }

    public URL getBaseURL() {
        return baseURL;
    }

    public Connector getConnector() {
        return connector;
    }

    public ContentHandlerRegistry getContentHandlerRegistry() {
        return contentHandlerRegistry;
    }

    public Repository getDefaultRepository() throws CannotConnectToServerException {
        return doGetRepositories()[0];
    }

    protected Repository[] repositories;

    protected Repository[] doGetRepositories() throws CannotConnectToServerException {
        if (repositories != null) {
            return repositories;
        }
        return repositories = connector.invoke(new RepositoriesCommand());
    }

    public Repository[] getRepositories() throws CannotConnectToServerException {
        return doGetRepositories();
    }

    public Repository getRepository(String id)
            throws NoSuchRepositoryException, CannotConnectToServerException {
        for (Repository repository : doGetRepositories()) {
            if (repository.getRepositoryId().equals(id)) {
                return repository;
            }
        }
        throw new NoSuchRepositoryException(baseURL, id);
    }

    public void message(String msg, int type) {
        throw new UnsupportedOperationException("not yet");
    }

    public String[] prompt(String message) {
        throw new UnsupportedOperationException("not yet");
    }

}
