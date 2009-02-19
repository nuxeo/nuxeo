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
package org.nuxeo.ecm.client.cm.app.abdera;

import java.util.Map;

import org.apache.abdera.ext.cmis.CmisObject;
import org.nuxeo.ecm.client.cm.ContentManager;
import org.nuxeo.ecm.client.cm.Repository;
import org.nuxeo.ecm.client.cm.Session;
import org.nuxeo.ecm.client.cm.app.APPContentManager;

/**
 * @author matic
 * 
 */
public class RepositoryAdapter implements Repository {

    protected final org.apache.abdera.model.Workspace atomWorkspace;

    protected final String repositoryId;

    protected final APPContentManager cm;

    public RepositoryAdapter(APPContentManager client,
            org.apache.abdera.model.Workspace atomWorkspace) {
        this.cm = client;
        this.repositoryId = atomWorkspace.getExtension(CmisObject.class).getObjectId();
        this.atomWorkspace = atomWorkspace;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public ContentManager getContentManager() {
        return cm;
    }
    
    /* (non-Javadoc)
     * @see org.nuxeo.ecm.client.cm.Repository#open()
     */
    public Session open() {
        // TODO Auto-generated method stub
        return null;
    }
    
    /* (non-Javadoc)
     * @see org.nuxeo.ecm.client.cm.Repository#open(java.util.Map)
     */
    public Session open(Map<String, Object> ctx) {
        // TODO Auto-generated method stub
        return null;
    }


}
