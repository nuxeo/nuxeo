/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.jaxrs.test;

import org.nuxeo.ecm.automation.client.jaxrs.RemoteException;
import org.nuxeo.ecm.automation.client.jaxrs.Session;
import org.nuxeo.ecm.automation.client.jaxrs.adapters.DocumentService;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.jaxrs.model.DocRef;
import org.nuxeo.ecm.automation.client.jaxrs.model.Document;
import org.nuxeo.ecm.automation.client.jaxrs.model.Documents;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class SampleOne {

    public static void main(String[] args) throws Exception {
        try {
            HttpAutomationClient client = new HttpAutomationClient(
                    "http://localhost:8080/nuxeo/site/automation");
            long start = System.currentTimeMillis();
            Session session = client.getSession("Administrator",
                    "Administrator");
            DocumentService rs = session.getAdapter(DocumentService.class);
            Document doc = (Document) session.newRequest(
                    DocumentService.FetchDocument).set("value",
                    "/default-domain").execute();
            System.out.println(doc);
            System.out.println(doc.getTitle());
            Documents docs = (Documents) session.newRequest(
                    DocumentService.GetDocumentChildren).setInput(doc).execute();
            System.out.println(docs);
            Document dd = null;
            for (Document d : docs) {
                if (d.getPath().endsWith("/workspaces")) {
                    dd = d;
                }
                System.out.println(d.getTitle() + " at " + d.getLastModified());
            }
            DocRef wsRef = new DocRef("/default-domain/workspaces");
            // doc =
            // (Document)session.newRequest(CreateDocument.ID).setInput(wsRef)
            // .set("type", "Workspace").set("name", "hello").execute();
            // System.out.println(doc + " - "+doc.getTitle());
            System.out.println("@@@@@@@@@@@@@@@@@@@");
            docs = (Documents) session.newRequest(
                    DocumentService.GetDocumentChildren).setInput(wsRef).execute();
            System.out.println(docs);
            for (Document d : docs) {
                System.out.println(d.getTitle() + " at " + d.getLastModified()
                        + " state: " + d.getState());
            }
            doc = rs.getDocument("/default-domain/workspaces");
            System.out.println("----------------------------");
            System.out.println(doc + " - " + doc.getTitle());
            System.out.println("@@@@@@@@@@@@@@@@@@@");
            System.out.println("took: "
                    + ((double) System.currentTimeMillis() - start) / 1000);
            client.shutdown();
        } catch (RemoteException e) {
            e.printStackTrace();
            System.out.println(e.getRemoteStackTrace());
        }
    }

}
