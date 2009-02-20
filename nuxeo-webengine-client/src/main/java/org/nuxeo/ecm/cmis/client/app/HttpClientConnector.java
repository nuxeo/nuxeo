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
package org.nuxeo.ecm.cmis.client.app;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Service;
import org.apache.abdera.model.Workspace;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.nuxeo.ecm.cmis.ContentManagerException;
import org.nuxeo.ecm.cmis.DocumentFeed;
import org.nuxeo.ecm.cmis.Repository;
import org.nuxeo.ecm.cmis.client.app.abdera.DocumentFeedAdapter;
import org.nuxeo.ecm.cmis.client.app.abdera.RepositoryAdapter;
import org.nuxeo.ecm.cmis.client.app.commands.AbstractCommand;
import org.nuxeo.ecm.cmis.client.app.commands.QueryCommand;
import org.nuxeo.ecm.cmis.client.app.commands.RepositoriesCommand;



/**
 * @author matic
 * 
 */
public class HttpClientConnector implements Connector {

    protected HttpClient support;
    protected APPContentManager cm;  

    public HttpClientConnector(APPContentManager cm) {
        this.cm = cm;
        this.support = new HttpClient();
    }
    
    public APPContentManager getContentManager() {
        return cm;
    }
    


    @SuppressWarnings("unchecked")
    public <T> T invoke(Command<T> command) throws ContentManagerException {
        if (command instanceof RepositoriesCommand) {
            return (T) doInvoke((RepositoriesCommand) command);
        } else if (command instanceof QueryCommand) {
            return (T) doInvoke((QueryCommand) command);
        }
        throw new UnsupportedOperationException("not yet");
    }

    protected <T extends Element> T doGet(AbstractCommand<?> command) throws ContentManagerException {
        String baseURL = cm.getBaseUrl();
        String url = command.formatURL(baseURL);
        GetMethod method = new GetMethod(url);
        InputStream bodyStream = null;
        try {
            bodyStream = method.getResponseBodyAsStream();
        } catch (IOException e) {
            throw new ContentManagerException("Cannot connect to " + url, e);
        }
        Document<T> document = new Abdera().getParser().parse(bodyStream);
        return document.getRoot();
    }
    
    protected DocumentFeed[] doInvoke(QueryCommand command) throws ContentManagerException {
        Feed atomFeed = this.doGet(command);
        DocumentFeed feed = new DocumentFeedAdapter(cm, atomFeed);
        return feed.toArray(new DocumentFeed[feed.size()]);
    }



    protected Repository[] doInvoke(RepositoriesCommand command) throws ContentManagerException {
        Service atomService = this.doGet(command);
        List<Workspace> atomWorkspaces = atomService.getWorkspaces();
        Repository[] repositories = new RepositoryAdapter[atomWorkspaces.size()];
        int index = 0;
        for (Workspace atomWorkspace:atomWorkspaces) {
            repositories[index++] = new RepositoryAdapter(cm, atomWorkspace);
        }
        return repositories;
    }
}
