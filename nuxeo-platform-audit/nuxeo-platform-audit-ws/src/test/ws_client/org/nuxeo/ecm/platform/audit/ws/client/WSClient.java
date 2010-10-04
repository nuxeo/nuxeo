/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: WSClient.java 19481 2007-05-27 10:50:10Z sfermigier $
 */

package org.nuxeo.ecm.platform.audit.ws.client;

import java.net.URL;

import org.nuxeo.ecm.platform.audit.api.AuditException;
import org.nuxeo.ecm.platform.audit.ws.api.jaws.ModifiedDocumentDescriptor;
import org.nuxeo.ecm.platform.audit.ws.jaws.WSAuditInterface;
import org.nuxeo.ecm.platform.audit.ws.jaws.WSAuditInterfaceBindingStub;
import org.nuxeo.ecm.platform.audit.ws.jaws.WSAuditServiceLocator;

/**
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class WSClient {

    public static String UUID;

    private WSAuditInterfaceBindingStub stub;

    // public static WSClient create(String url) {
    // Proxy.newProxyInstance(loader, new Class[] {NuxeoRemoting.class}, )
    // }

    public static WSAuditInterface create() throws Exception {
        WSAuditServiceLocator locator = new WSAuditServiceLocator();
        WSAuditInterfaceBindingStub _stub = new WSAuditInterfaceBindingStub(
                new URL(locator.getWSAuditInterfacePortAddress()), locator);
        _stub.setPortName(locator.getWSAuditInterfacePortWSDDServiceName());
        return _stub;
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws AuditException {
        try {

            WSAuditInterface remoting = create();
            if (remoting == null)
                new Exception("remoting init failed");

            System.out.println("Starting test suite.....");

            // connection
            String sessionId = remoting.connect("Administrator",
                    "Administrator");
            System.out.println("Connected....");

            System.out.println("No date range.");
            ModifiedDocumentDescriptor[] docs = remoting.listModifiedDocuments(
                    sessionId, null);
            printDocuments(docs);

            System.out.println("Docs modified during the last 72 hours....");
            docs = remoting.listModifiedDocuments(sessionId, "72h");
            printDocuments(docs);

            System.out.println("Docs modified during the last 2 hours....");
            docs = remoting.listModifiedDocuments(sessionId, "2h");
            printDocuments(docs);

            System.out.println("Docs modified during the last 1 hours....");
            docs = remoting.listModifiedDocuments(sessionId, "1h");
            printDocuments(docs);

            System.out.println("Docs modified during the last 59 minutes....");
            docs = remoting.listModifiedDocuments(sessionId, "59m");
            printDocuments(docs);

            System.out.println("Docs modified during the last 45 minutes....");
            docs = remoting.listModifiedDocuments(sessionId, "45m");
            printDocuments(docs);

            System.out.println("Docs modified during the last 2 minutes....");
            docs = remoting.listModifiedDocuments(sessionId, "2m");
            printDocuments(docs);

            remoting.disconnect(sessionId);
            System.out.println("Disconnected....");

            System.out.println("done.....");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static final void printDocuments(ModifiedDocumentDescriptor[] docs) {
        System.out.println("###############################################");
        if (docs == null || docs.length == 0) {
            System.out.println("No documents found...");
        } else {
            for (ModifiedDocumentDescriptor doc : docs) {
                System.out.println("Doc UUID=" + doc.getUUID());
                System.out.println("Doc type=" + doc.getType());
                System.out.println("Doc modified" + doc.getModified());
            }
        }
        System.out.println("###############################################");
    }

}
