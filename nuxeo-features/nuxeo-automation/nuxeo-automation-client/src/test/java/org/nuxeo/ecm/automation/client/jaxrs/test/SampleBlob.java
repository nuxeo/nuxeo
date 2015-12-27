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

import java.io.File;

import org.nuxeo.ecm.automation.client.Constants;
import org.nuxeo.ecm.automation.client.RemoteException;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.adapters.DocumentService;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.client.model.Document;
import org.nuxeo.ecm.automation.client.model.FileBlob;
import org.nuxeo.ecm.automation.client.model.PropertyMap;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class SampleBlob {

    public static void main(String[] args) throws Exception {
        try {
            HttpAutomationClient client = new HttpAutomationClient("http://192.168.1.200:8080/nuxeo/site/automation");
            long start = System.currentTimeMillis();
            // SessionImpl session = (SessionImpl)client.getSession(null);
            Session session = client.getSession("Administrator", "Administrator");
            // FileBlob fb = new FileBlob(new
            // File("/Users/bstefanescu/operations.jpg"));
            FileBlob fb = new FileBlob(new File("/Users/fermigier/Pictures/bart.jpg"));
            fb.setMimeType("image/jpeg");

            // TODO avoid getting the output from server
            Blob blob = (Blob) session.newRequest("Blob.Attach").setHeader(Constants.HEADER_NX_VOIDOP, "true").setInput(
                    fb).set("document", "/titi").execute();
            System.out.println(blob);
            // System.out.println(((FileBlob)blob).getFile());

            Document doc = (Document) session.newRequest(DocumentService.FetchDocument).setHeader(
                    Constants.HEADER_NX_SCHEMAS, "*").set("value", "/default-domain/workspaces/myws/file").execute();

            System.out.println(doc);
            System.out.println(doc.getProperties().map());
            PropertyMap map = doc.getProperties().getMap("file:content");
            System.out.println("----------");
            String path = map.getString("data");
            blob = session.getFile(path);
            System.out.println(((FileBlob) blob).getFile());
            System.out.println("----------");
            System.out.println(map);

            System.out.println("took: " + ((double) System.currentTimeMillis() - start) / 1000);

            start = System.currentTimeMillis();
            for (int i = 0; i < 60; i++) {
                doc = (Document) session.newRequest(DocumentService.FetchDocument).setHeader(
                        Constants.HEADER_NX_SCHEMAS, "*").set("value", "/default-domain/workspaces/myws/file").execute();
            }
            System.out.println("60 full docs took: " + ((double) System.currentTimeMillis() - start) / 1000);
            client.shutdown();
        } catch (RemoteException e) {
            e.printStackTrace();
            System.out.println(e.getRemoteStackTrace());
        }
    }

}
