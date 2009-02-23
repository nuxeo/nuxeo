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
 *     bstefanescu
 */
package org.nuxeo.ecm.cmis.client.app;

import org.nuxeo.ecm.cmis.ContentManager;
import org.nuxeo.ecm.cmis.ContentManagerException;
import org.nuxeo.ecm.cmis.NoSuchRepositoryException;
import org.nuxeo.ecm.cmis.Repository;
import org.nuxeo.ecm.cmis.client.app.httpclient.HttpClientConnector;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class APPContentManager implements ContentManager {

    protected String baseUrl;
    protected Connector connector;

    protected ContentHandlerRegistry handlerRegistry;
    
    public APPContentManager(String url) {
        this (url, null);
    }

    protected APPContentManager(String url, Connector connector) {
        this.baseUrl = url;
        this.connector = connector == null ? new HttpClientConnector(this) : connector;
        this.handlerRegistry = new ContentHandlerRegistry();
    }
    
    
    public ContentHandlerRegistry getContentHandlerRegistry() {
        return handlerRegistry;
    }
    
    public <T> ContentHandler<T> getContentHandler(Class<T> clazz) {
        return handlerRegistry.getHandler(clazz);
    }

    public void registerContentHandler(ContentHandler<?> handler) {
        handlerRegistry.registerHandler(handler);
    }

    public String getBaseUrl() {
        return baseUrl;
    }
    
    public Connector getConnector() {
        return connector;
    }
    


    public Repository[] getRepositories() throws ContentManagerException {
        Request req = new Request(getBaseUrl());
        Response resp = connector.get(req);
        APPServiceDocument app = resp.getContent(APPServiceDocument.class);
        // TODO app.getWorkspaces();
        return null;
    }

    public Repository getRepository(String id)
            throws NoSuchRepositoryException, ContentManagerException {
        for (Repository repository : getRepositories()) {
            if (repository.getRepositoryId().equals(id)) {
                return repository;
            }
        }
        throw new NoSuchRepositoryException(baseUrl, id);
    }    
    
    public Repository getDefaultRepository() throws ContentManagerException {
        Repository[] repos = getRepositories();
        if (repos != null && repos.length > 0) {
            return repos[0];
        }
        throw new NoSuchRepositoryException(baseUrl, "default");
    }
    
    
}
