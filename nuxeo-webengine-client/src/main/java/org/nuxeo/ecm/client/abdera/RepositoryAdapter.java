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
package org.nuxeo.ecm.client.abdera;

import org.apache.abdera.ext.cmis.CmisObject;
import org.nuxeo.ecm.client.ConnectorException;
import org.nuxeo.ecm.client.ContentManager;
import org.nuxeo.ecm.client.DiscoveryService;
import org.nuxeo.ecm.client.DocumentEntry;
import org.nuxeo.ecm.client.NavigationService;
import org.nuxeo.ecm.client.ObjectService;
import org.nuxeo.ecm.client.Repository;
import org.nuxeo.ecm.client.commands.QueryCommand;

/**
 * @author matic
 * 
 */
public class RepositoryAdapter implements Repository, DiscoveryService {

    protected final org.apache.abdera.model.Workspace atomWorkspace;

    protected final String repositoryId;

    protected final ContentManager contentManager;

    public RepositoryAdapter(ContentManager client,
            org.apache.abdera.model.Workspace atomWorkspace) {
        this.contentManager = client;
        this.repositoryId = atomWorkspace.getExtension(CmisObject.class).getObjectId();
        this.atomWorkspace = atomWorkspace;
    }

    public ObjectService getObjectService() {
        throw new UnsupportedOperationException("not yet");
    }

    public String getRepositoryId() {
        return repositoryId;
    }
    
    public NavigationService getNavigationService() {
        throw new UnsupportedOperationException("not yet");
    }

    public DocumentEntry[] getQueries() throws ConnectorException {
        return contentManager.getConnector().invoke(new QueryCommand(repositoryId));
    }

    public DocumentEntry getRoot() {
        throw new UnsupportedOperationException("not yet");
    }

    public <T> T getService(Class<T> serviceType) {
        throw new UnsupportedOperationException("not yet");
    }


    public DiscoveryService getDiscoveryService() {
        return this;
    }

}
