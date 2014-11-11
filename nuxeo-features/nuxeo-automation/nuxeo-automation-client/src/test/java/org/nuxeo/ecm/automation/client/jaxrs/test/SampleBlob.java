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
public class SampleBlob {

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
