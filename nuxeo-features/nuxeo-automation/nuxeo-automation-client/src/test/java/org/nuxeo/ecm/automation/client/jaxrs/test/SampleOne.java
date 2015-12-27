/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.jaxrs.test;

import org.nuxeo.ecm.automation.client.RemoteException;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.adapters.DocumentService;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.DocRef;
import org.nuxeo.ecm.automation.client.model.Document;
import org.nuxeo.ecm.automation.client.model.Documents;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class SampleOne {

    public static void main(String[] args) throws Exception {
        try {
            HttpAutomationClient client = new HttpAutomationClient("http://localhost:8080/nuxeo/site/automation");
            long start = System.currentTimeMillis();
            Session session = client.getSession("Administrator", "Administrator");
            DocumentService rs = session.getAdapter(DocumentService.class);
            Document doc = (Document) session.newRequest(DocumentService.FetchDocument).set("value", "/default-domain").execute();
            System.out.println(doc);
            System.out.println(doc.getTitle());
            Documents docs = (Documents) session.newRequest(DocumentService.GetDocumentChildren).setInput(doc).execute();
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
            docs = (Documents) session.newRequest(DocumentService.GetDocumentChildren).setInput(wsRef).execute();
            System.out.println(docs);
            for (Document d : docs) {
                System.out.println(d.getTitle() + " at " + d.getLastModified() + " state: " + d.getState());
            }
            doc = rs.getDocument("/default-domain/workspaces");
            System.out.println("----------------------------");
            System.out.println(doc + " - " + doc.getTitle());
            System.out.println("@@@@@@@@@@@@@@@@@@@");
            System.out.println("took: " + ((double) System.currentTimeMillis() - start) / 1000);
            client.shutdown();
        } catch (RemoteException e) {
            e.printStackTrace();
            System.out.println(e.getRemoteStackTrace());
        }
    }

}
