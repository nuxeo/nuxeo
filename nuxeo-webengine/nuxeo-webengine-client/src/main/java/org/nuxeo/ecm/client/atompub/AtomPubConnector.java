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
package org.nuxeo.ecm.client.atompub;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.apache.abdera.Abdera;
import org.apache.abdera.ext.cmis.CmisExtensionFactory;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Service;
import org.apache.abdera.model.Workspace;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.nuxeo.ecm.client.CannotConnectToServerException;
import org.nuxeo.ecm.client.Command;
import org.nuxeo.ecm.client.Connector;
import org.nuxeo.ecm.client.ContentManager;
import org.nuxeo.ecm.client.DocumentFeed;
import org.nuxeo.ecm.client.QueryEntry;
import org.nuxeo.ecm.client.Repository;
import org.nuxeo.ecm.client.abdera.DocumentFeedAdapter;
import org.nuxeo.ecm.client.abdera.QueryEntryTransformer;
import org.nuxeo.ecm.client.abdera.RepositoryAdapter;
import org.nuxeo.ecm.client.commands.AbstractCommand;
import org.nuxeo.ecm.client.commands.GetDocumentFeedCommand;
import org.nuxeo.ecm.client.commands.GetQueriesCommand;
import org.nuxeo.ecm.client.commands.RefreshDocumentFeedCommand;
import org.nuxeo.ecm.client.commands.RepositoriesCommand;

/**
 * @author matic
 *
 */
public class AtomPubConnector implements Connector {

    protected HttpClient httpClient;

    protected Abdera abdera;

    public void init(ContentManager contentManager) {
        this.contentManager = contentManager;
        this.httpClient = new HttpClient();
        this.abdera = Abdera.getInstance();
        this.abdera.getConfiguration().addExtensionFactory(
                new CmisExtensionFactory());
    }

    protected ContentManager contentManager;

    @SuppressWarnings("unchecked")
    public <T> T invoke(Command<T> command)
            throws CannotConnectToServerException {
        if (command instanceof RepositoriesCommand) {
            return (T) doInvoke((RepositoriesCommand) command);
        } else if (command instanceof GetQueriesCommand) {
            return (T) doInvoke((GetQueriesCommand) command);
        } else if (command instanceof GetDocumentFeedCommand) {
            return (T) doInvoke((GetDocumentFeedCommand)command);
        } else if (command instanceof RefreshDocumentFeedCommand) {
            return (T) doInvoke((RefreshDocumentFeedCommand) command);
        }
        throw new UnsupportedOperationException("not yet");
    }






    protected <T extends Element> T doGet(AbstractCommand<?> command)
            throws CannotConnectToServerException {
        URL baseURL = contentManager.getBaseURL();
        String url = command.formatURL(baseURL);
        GetMethod method = new GetMethod(url);
        InputStream bodyStream = null;
        String serverTag = command.getServerTag();
        if (serverTag != null) {
            method.setRequestHeader(new Header("If-Match", "*"));
            method.setRequestHeader(new Header("If-Range",serverTag));
        }
        try {
            httpClient.executeMethod(method);
            if (method.getStatusCode() == 304) {
                return null;
            }
            bodyStream = method.getResponseBodyAsStream();
        } catch (Exception e) {
            throw CannotConnectToServerException.wrap("Cannot connect to "
                    + url, e);
        }
        Header header = method.getResponseHeader("ETag");
        if (header != null) {
            command.setServerTag(header.getValue());
        }
        Document<T> document = abdera.getParser().parse(bodyStream);
        return document.getRoot();
    }

    protected List<QueryEntry> doInvoke(GetQueriesCommand command)
            throws CannotConnectToServerException {
        Feed atomFeed = this.doGet(command);
        return QueryEntryTransformer.transformEntries(atomFeed.getEntries(), contentManager);
    }

    protected Repository[] doInvoke(RepositoriesCommand command)
            throws CannotConnectToServerException {
        Service atomService = this.doGet(command);
        List<Workspace> atomWorkspaces = atomService.getWorkspaces();
        Repository[] repositories = new RepositoryAdapter[atomWorkspaces.size()];
        int index = 0;
        for (Workspace atomWorkspace : atomWorkspaces) {
            repositories[index++] = new RepositoryAdapter(contentManager,
                    atomWorkspace);
        }
        return repositories;
    }

    protected DocumentFeed doInvoke(GetDocumentFeedCommand command) throws CannotConnectToServerException {
        Feed atomFeed = this.doGet(command);
        return new DocumentFeedAdapter(contentManager,atomFeed,command.getServerTag());
    }

    private DocumentFeed doInvoke(RefreshDocumentFeedCommand command) throws CannotConnectToServerException {
        Feed atomFeed = this.doGet(command);
        if (atomFeed == null) {
            return null;
        }
        return new DocumentFeedAdapter(contentManager,atomFeed,command.getServerTag(),command.getLastEntries());
    }

}
