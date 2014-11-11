/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.shell.automation;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.automation.client.jaxrs.AutomationClient;
import org.nuxeo.ecm.automation.client.jaxrs.Session;
import org.nuxeo.ecm.automation.client.jaxrs.adapters.DocumentService;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.jaxrs.model.DocRef;
import org.nuxeo.ecm.automation.client.jaxrs.model.Document;
import org.nuxeo.ecm.automation.client.jaxrs.model.PathRef;
import org.nuxeo.ecm.shell.ShellException;
import org.nuxeo.ecm.shell.utils.Path;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class RemoteContext {

    protected AutomationShell shell;

    protected HttpAutomationClient client;

    protected Session session;

    protected DocumentService ds;

    protected Document doc;

    protected List<Document> stack;

    protected String userName;

    protected String host;

    public RemoteContext(AutomationShell shell, HttpAutomationClient client,
            Session session) throws Exception {
        this.shell = shell;
        this.client = client;
        this.session = session;
        ds = session.getAdapter(DocumentService.class);
        shell.putContextObject(RemoteContext.class, this);
        shell.putContextObject(AutomationClient.class, client);
        shell.putContextObject(Session.class, session);
        shell.putContextObject(DocumentService.class, ds);
        stack = new ArrayList<Document>();
        // cd into root
        doc = ds.getRootDocument();
        userName = session.getLogin().getUsername();
        host = new URL(client.getBaseUrl()).getHost();
    }

    public String getUserName() {
        return userName;
    }

    public String getHost() {
        return host;
    }

    public void dispose() {
        shell.removeContextObject(RemoteContext.class);
        shell.removeContextObject(AutomationClient.class);
        shell.removeContextObject(Session.class);
        shell.removeContextObject(DocumentService.class);
    }

    public AutomationShell getShell() {
        return shell;
    }

    public HttpAutomationClient getClient() {
        return client;
    }

    public Session getSession() {
        return session;
    }

    public Document getDocument() {
        return doc;
    }

    public Path resolvePath(String path) {
        if (path == null) {
            return new Path(getDocument().getPath());
        }
        if (path.startsWith("/")) {
            return new Path(path);
        } else {
            return new Path(doc.getPath()).append(path);
        }
    }

    public DocRef resolveRef(String path) {
        if (path == null) {
            return getDocument();
        }
        if (path.startsWith("doc:")) {
            return DocRef.newRef(path.substring("doc:".length()));
        }
        return new PathRef(resolvePath(path).toString());
    }

    public Document resolveDocument(String path) {
        return resolveDocument(path, null);
    }

    public Document resolveDocument(String path, String schemas) {
        DocRef ref = null;
        try {
            ref = resolveRef(path);
            return ds.getDocument(ref, schemas);
        } catch (Exception e) {
            throw new ShellException("Failed to fetch document: " + ref);
        }
    }

    public void setDocument(Document doc) {
        this.doc = doc;
    }

    public Document peekDocument() {
        if (!stack.isEmpty()) {
            return stack.get(stack.size() - 1);
        }
        return null;
    }

    public Document pushDocument(Document doc) {
        setDocument(doc);
        Document old = peekDocument();
        stack.add(doc);
        return old;
    }

    public Document popDocument() {
        if (!stack.isEmpty()) {
            Document doc = stack.remove(stack.size() - 1);
            setDocument(peekDocument());
            return doc;
        }
        return null;
    }

    public DocumentService getDocumentService() {
        return ds;
    }

    public List<Document> getStack() {
        return stack;
    }
}
