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

import java.util.List;

import org.nuxeo.ecm.cmis.DiscoveryService;
import org.nuxeo.ecm.cmis.DocumentEntry;
import org.nuxeo.ecm.cmis.NavigationService;
import org.nuxeo.ecm.cmis.ObjectService;
import org.nuxeo.ecm.cmis.Query;
import org.nuxeo.ecm.cmis.common.AbstractSession;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class APPSession extends AbstractSession {

    protected APPContentManager cm;
    protected Connector connector;

    public APPSession(AppRepository repo) {
        super (repo);
        cm = (APPContentManager)repo.getContentManager();
        this.connector = cm.getConnector(); // TODO clone connector to be able to use different logins
    }

    public Connector getConnector() {
        return connector;
    }

    public String getBaseUrl() {
        return cm.getBaseUrl();
    }

    public DiscoveryService getDiscoveryService() {
        // TODO Auto-generated method stub
        return null;
    }

    public NavigationService getNavigationService() {
        // TODO Auto-generated method stub
        return null;
    }

    public ObjectService getObjectService() {
        // TODO Auto-generated method stub
        return null;
    }

    public List<Query> getQuerys() {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentEntry getRoot() {
        // TODO Auto-generated method stub
        return null;
    }



}
