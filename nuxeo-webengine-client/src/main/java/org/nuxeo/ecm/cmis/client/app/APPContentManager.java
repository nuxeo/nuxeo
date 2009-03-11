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


import org.nuxeo.ecm.cmis.ContentManagerException;
import org.nuxeo.ecm.cmis.NoSuchRepositoryException;
import org.nuxeo.ecm.cmis.Repository;
import org.nuxeo.ecm.cmis.client.app.httpclient.HttpClientConnector;
import org.nuxeo.ecm.cmis.common.AbstractContentManager;
import org.nuxeo.ecm.cmis.common.AdapterManager;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class APPContentManager extends AbstractContentManager {

    protected String baseUrl;
    protected AdapterManager adapterMgr;
    protected Connector connector;

    protected SerializationManager serializationMgr;

    public APPContentManager(String url) {
        this (url, null, null);
    }

    protected APPContentManager(String url, Connector connector, SerializationManager serializationMgr) {
        this.baseUrl = url;
        this.connector = connector == null ? createConnector() : connector;
        this.serializationMgr = serializationMgr == null ? createSerializationManager() : serializationMgr;
    }

    protected SerializationManager createSerializationManager() {
        return new DefaultSerializationManager();
    }

    protected Connector createConnector() {
        return new HttpClientConnector(this);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Connector getConnector() {
        return connector;
    }

    public SerializationManager getSerializationManager() {
        return serializationMgr;
    }

    public void registerSerializationHandler(SerializationHandler<?> handler) {
        serializationMgr.registerHandler(handler);
    }

    public void unregisterSerializationHandler(Class<?> clazz) {
        serializationMgr.unregisterHandler(clazz);
    }

    public Repository[] getRepositories() throws ContentManagerException {
        Request req = new Request(getBaseUrl());
        Response resp = connector.get(req);
        if (!resp.isOk()) {
            throw new ContentManagerException("Remote server returned error code: "+resp.getStatusCode());
        }
        APPServiceDocument app = resp.getEntity(this, APPServiceDocument.class);
        return app.getRepositories();
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
