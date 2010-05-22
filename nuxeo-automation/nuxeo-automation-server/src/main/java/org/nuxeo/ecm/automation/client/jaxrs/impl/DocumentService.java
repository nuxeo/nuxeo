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
package org.nuxeo.ecm.automation.client.jaxrs.impl;

import org.nuxeo.ecm.automation.client.jaxrs.OperationRequest;
import org.nuxeo.ecm.automation.client.jaxrs.Session;
import org.nuxeo.ecm.automation.client.jaxrs.model.DocRef;
import org.nuxeo.ecm.automation.client.jaxrs.model.Document;
import org.nuxeo.ecm.automation.client.jaxrs.model.Documents;
import org.nuxeo.ecm.automation.client.jaxrs.model.Properties;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.core.operations.document.FetchDocument;
import org.nuxeo.ecm.automation.core.operations.document.GetDocumentChildren;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DocumentService {

    protected Session session;

    public DocumentService(Session session) {
        this.session = session;
    }

    public Session getSession() {
        return session;
    }

    public Document getDocument(String ref) throws Exception {
        return (Document)session.newRequest(FetchDocument.ID).set("value", ref).execute();
    }

    public Document createDocument(Document parent, String type, String name) throws Exception {
        return createDocument(parent, type, name, null);
    }

    public Document createDocument(Document parent, String type, String name, Properties properties) throws Exception {
        OperationRequest req = session.newRequest(CreateDocument.ID).setInput(parent).set("type", type).set("name", name);
        if (properties != null && properties.size() > 0) {
            req.set("properties", properties);
        }
        return (Document)req.execute();
    }

    public Documents getChildren(Document doc) throws Exception {
        return (Documents)session.newRequest(GetDocumentChildren.ID).setInput(doc).execute();
    }

    public Documents getChildren(DocRef docRef) throws Exception {
        return (Documents)session.newRequest(GetDocumentChildren.ID).setInput(docRef).execute();
    }

}
