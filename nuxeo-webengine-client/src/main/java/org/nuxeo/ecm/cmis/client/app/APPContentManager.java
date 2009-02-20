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
import org.nuxeo.ecm.cmis.client.app.commands.RepositoriesCommand;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class APPContentManager implements ContentManager {

    protected String baseUrl;
    protected Connector connector;

    protected Repository[] repositories;

    public APPContentManager(String url) {
        this (url, null);
    }

    protected APPContentManager(String url, Connector connector) {
        this.baseUrl = url;
        this.connector = connector == null ? new HttpClientConnector(this) : connector;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public Connector getConnector() {
        return connector;
    }
    

    protected Repository[] doGetRepositories() throws ContentManagerException {
        if (repositories != null) {
            return repositories;
        }
        return repositories = connector.invoke(new RepositoriesCommand());
    }

    public Repository[] getRepositories() throws ContentManagerException {
        return doGetRepositories();
    }

    public Repository getRepository(String id)
            throws NoSuchRepositoryException, ContentManagerException {
        for (Repository repository : doGetRepositories()) {
            if (repository.getRepositoryId().equals(id)) {
                return repository;
            }
        }
        throw new NoSuchRepositoryException(baseUrl, id);
    }    
    
    /* (non-Javadoc)
     * @see org.nuxeo.ecm.client.cm.ContentManager#getDefaultRepository()
     */
    public Repository getDefaultRepository() throws ContentManagerException {
        if (repositories.length > 0) {
            return repositories[0]; 
        }
        throw new NoSuchRepositoryException(baseUrl, "default");
    }
    
    /* (non-Javadoc)
     * @see org.nuxeo.ecm.client.cm.ContentManager#reload()
     */
    public void reload() {
        // TODO Auto-generated method stub        
    }
    
}
