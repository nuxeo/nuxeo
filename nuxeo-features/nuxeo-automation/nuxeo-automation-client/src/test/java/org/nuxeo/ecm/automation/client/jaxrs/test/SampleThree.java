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

import org.nuxeo.ecm.automation.client.Constants;
import org.nuxeo.ecm.automation.client.RemoteException;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.adapters.DocumentService;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.Document;
import org.nuxeo.ecm.automation.client.model.Documents;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class SampleThree {

    public static void main(String[] args) throws Exception {
        try {
            // create the client
            HttpAutomationClient client = new HttpAutomationClient("http://localhost:8080/nuxeo/site/automation");
            // get an Administrator session
            Session session = client.getSession("Administrator", "Administrator");
            // get the /default-domain/workspaces document
            Document doc = (Document) session.newRequest("Document.Fetch").set("value", "/default-domain/workspaces").execute();
            System.out.println(doc);
            System.out.println(doc.getTitle());

            // create a new workspace (inside /default-domain/workspaces)
            Document myWs = (Document) session.newRequest("Document.Create").setInput(doc).set("type", "Workspace").set(
                    "name", "MyWorkspace").set("properties",
                    "dc:title=My Workspace\ndc:description=This is my workspace").execute();
            System.out.println(myWs.getTitle());

            // now list the children in /default-domain/workspaces
            Documents docs = (Documents) session.newRequest(DocumentService.GetDocumentChildren).setInput(doc).setHeader(
                    Constants.HEADER_NX_SCHEMAS, "*").execute();
            System.out.println(docs);

            // list children titles
            for (Document d : docs) {
                System.out.println(d.getTitle() + " at " + d.getLastModified());
            }

            // shutdown the client
            client.shutdown();
        } catch (RemoteException e) {
            e.printStackTrace();
            System.out.println(e.getRemoteStackTrace());
        }
    }

}
