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
package org.nuxeo.ecm.client.httpclient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Service;
import org.apache.abdera.model.Workspace;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.nuxeo.ecm.client.Command;
import org.nuxeo.ecm.client.Connector;
import org.nuxeo.ecm.client.ConnectorException;
import org.nuxeo.ecm.client.ContentManager;
import org.nuxeo.ecm.client.DocumentFeed;
import org.nuxeo.ecm.client.Repository;
import org.nuxeo.ecm.client.abdera.DocumentFeedAdapter;
import org.nuxeo.ecm.client.abdera.RepositoryAdapter;
import org.nuxeo.ecm.client.commands.AbstractCommand;
import org.nuxeo.ecm.client.commands.QueryCommand;
import org.nuxeo.ecm.client.commands.RepositoriesCommand;

/**
 * @author matic
 * 
 */
public class HttpClientConnector implements Connector {

    protected HttpClient support;

    public void init(ContentManager contentManager) {
        this.contentManager = contentManager;
        this.support = new HttpClient();
    }

    protected ContentManager contentManager;

    @SuppressWarnings("unchecked")
    public <T> T invoke(Command<T> command) {
        if (command instanceof RepositoriesCommand) {
            return (T) doInvoke((RepositoriesCommand) command);
        } else if (command instanceof QueryCommand) {
            return (T) doInvoke((QueryCommand) command);
        }
        throw new UnsupportedOperationException("not yet");
    }

    protected <T extends Element> T doGet(AbstractCommand<?> command) {
        URL baseURL = contentManager.getBaseURL();
        String url = command.formatURL(baseURL);
        GetMethod method = new GetMethod(url);
        InputStream bodyStream = null;
        try {
            bodyStream = method.getResponseBodyAsStream();
        } catch (IOException e) {
            ConnectorException.wrap("Cannot connect to " + url, e);
        }
        Document<T> document = new Abdera().getParser().parse(bodyStream);
        return document.getRoot();
    }
    
    protected DocumentFeed[] doInvoke(QueryCommand command) {
        Feed atomFeed = this.doGet(command);
        DocumentFeed feed = new DocumentFeedAdapter(contentManager, atomFeed);
        return feed.toArray(new DocumentFeed[feed.size()]);
    }



    protected Repository[] doInvoke(RepositoriesCommand command) {
        Service atomService = this.doGet(command);
        List<Workspace> atomWorkspaces = atomService.getWorkspaces();
        Repository[] repositories = new RepositoryAdapter[atomWorkspaces.size()];
        int index = 0;
        for (Workspace atomWorkspace:atomWorkspaces) {
            repositories[index++] = new RepositoryAdapter(contentManager,atomWorkspace);
        }
        return repositories;
    }
}
