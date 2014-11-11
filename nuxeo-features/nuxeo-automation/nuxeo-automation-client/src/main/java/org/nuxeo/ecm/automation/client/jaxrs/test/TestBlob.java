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
package org.nuxeo.ecm.automation.client.jaxrs.test;

import java.io.File;

import org.nuxeo.ecm.automation.client.jaxrs.Constants;
import org.nuxeo.ecm.automation.client.jaxrs.RemoteException;
import org.nuxeo.ecm.automation.client.jaxrs.Session;
import org.nuxeo.ecm.automation.client.jaxrs.adapters.DocumentService;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.jaxrs.model.Blob;
import org.nuxeo.ecm.automation.client.jaxrs.model.Document;
import org.nuxeo.ecm.automation.client.jaxrs.model.FileBlob;
import org.nuxeo.ecm.automation.client.jaxrs.model.PropertyMap;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TestBlob {

    public static void main(String[] args) throws Exception {
        try {
            HttpAutomationClient client = new HttpAutomationClient(
                    "http://192.168.1.200:8080/nuxeo/site/automation");
            long start = System.currentTimeMillis();
            // SessionImpl session = (SessionImpl)client.getSession(null);
            Session session = client.getSession("Administrator",
                    "Administrator");
            // FileBlob fb = new FileBlob(new
            // File("/Users/bstefanescu/operations.jpg"));
            FileBlob fb = new FileBlob(new File(
                    "/Users/fermigier/Pictures/bart.jpg"));
            fb.setMimeType("image/jpeg");

            // TODO avoid getting the output from server
            Blob blob = (Blob) session.newRequest("Blob.Attach").setHeader(
                    Constants.HEADER_NX_VOIDOP, "true").setInput(fb).set(
                    "document", "/titi").execute();
            System.out.println(blob);
            // System.out.println(((FileBlob)blob).getFile());

            Document doc = (Document) session.newRequest(
                    DocumentService.FetchDocument).setHeader(
                    Constants.HEADER_NX_SCHEMAS, "*").set("value",
                    "/default-domain/workspaces/myws/file").execute();

            System.out.println(doc);
            System.out.println(doc.getProperties().map());
            PropertyMap map = doc.getProperties().getMap("file:content");
            System.out.println("----------");
            String path = map.getString("data");
            blob = session.getFile(path);
            System.out.println(((FileBlob) blob).getFile());
            System.out.println("----------");
            System.out.println(map);

            System.out.println("took: "
                    + ((double) System.currentTimeMillis() - start) / 1000);

            start = System.currentTimeMillis();
            for (int i = 0; i < 60; i++) {
                doc = (Document) session.newRequest(
                        DocumentService.FetchDocument).setHeader(
                        Constants.HEADER_NX_SCHEMAS, "*").set("value",
                        "/default-domain/workspaces/myws/file").execute();
            }
            System.out.println("60 full docs took: "
                    + ((double) System.currentTimeMillis() - start) / 1000);
            client.shutdown();
        } catch (RemoteException e) {
            e.printStackTrace();
            System.out.println(e.getRemoteStackTrace());
        }
    }

}
