/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     slacoin
 */
package org.nuxeo.ecm.automation.client.jaxrs.test;

import org.nuxeo.ecm.automation.client.jaxrs.RemoteException;
import org.nuxeo.ecm.automation.client.jaxrs.Session;
import org.nuxeo.ecm.automation.client.jaxrs.adapters.DocumentService;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.jaxrs.model.Document;
import org.nuxeo.ecm.automation.client.jaxrs.model.Documents;
import org.nuxeo.ecm.automation.client.jaxrs.spi.auth.PortalSSOAuthInterceptor;

public class SampleSSOPortal {

        public static void main(String[] args) throws Exception {
        try {
            HttpAutomationClient client = new HttpAutomationClient("http://localhost:8080/nuxeo/site/automation");
            client.setRequestInterceptor(new PortalSSOAuthInterceptor("nuxeo5secretkey", "Administrator"));
            Session session = client.getSession();
            DocumentService rs = session.getAdapter(DocumentService.class);
            Documents docs = rs.query("SELECT * from Workspace");
            System.out.println(docs);
            for (Document d : docs) {
                System.out.println(d.getTitle() + " at " + d.getLastModified());
            }
            client.shutdown();
        } catch (RemoteException e) {
            e.printStackTrace();
            System.out.println(e.getRemoteStackTrace());
        }
    }


}
