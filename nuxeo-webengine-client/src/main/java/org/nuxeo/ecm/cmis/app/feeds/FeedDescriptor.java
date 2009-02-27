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
package org.nuxeo.ecm.cmis.app.feeds;

import org.nuxeo.ecm.cmis.ContentManagerException;
import org.nuxeo.ecm.cmis.DocumentEntry;
import org.nuxeo.ecm.cmis.client.app.APPSession;
import org.nuxeo.ecm.cmis.client.app.Feed;
import org.nuxeo.ecm.cmis.client.app.Request;
import org.nuxeo.ecm.cmis.client.app.Response;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FeedDescriptor {

    protected FeedService service;
    protected String url;
    protected String title;
    protected int pageSize; // recommended page size
    
    public FeedDescriptor(FeedService service, String url, String title, int pageSize) {
        this.service = service;
        this.url = url;
        this.title = title;
        this.pageSize = pageSize;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getUrl() {
        return url;
    }
    
    public int getPageSize() {
        return pageSize;
    }
    
    public Feed<DocumentEntry> query() throws ContentManagerException {
        APPSession session = (APPSession)service.getSession();
        Request req = new Request(url);
        Response resp = session.getConnector().get(req);
        return resp.getFeed(service.getSession(), DocumentEntry.class);
    }

    public Feed<DocumentEntry> query(String query) throws ContentManagerException {
        APPSession session = (APPSession)service.getSession();
        Request req = new Request(url);
        req.setParameter("query", query);
        Response resp = session.getConnector().get(req);
        return resp.getFeed(service.getSession(), DocumentEntry.class);        
    }

    public Feed<DocumentEntry> query(int offset, int pageSize) throws ContentManagerException {
        APPSession session = (APPSession)service.getSession();
        Request req = new Request(url);
        req.setParameter("offset", Integer.toString(offset));
        req.setParameter("length", Integer.toString(pageSize));
        Response resp = session.getConnector().get(req);
        return resp.getFeed(service.getSession(), DocumentEntry.class);
    }

    public Feed<DocumentEntry> query(String query, int offset, int pageSize) throws ContentManagerException {
        APPSession session = (APPSession)service.getSession();
        Request req = new Request(url);
        req.setParameter("query", query);
        req.setParameter("offset", Integer.toString(offset));
        req.setParameter("length", Integer.toString(pageSize));
        Response resp = session.getConnector().get(req);
        return resp.getFeed(service.getSession(), DocumentEntry.class);    
    }

}
