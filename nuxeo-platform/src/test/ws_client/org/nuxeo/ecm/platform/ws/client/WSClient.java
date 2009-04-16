/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.ws.client;

import java.net.URL;
import java.util.Arrays;

import org.nuxeo.ecm.core.api.security.jaws.ACE;
import org.nuxeo.ecm.platform.api.ws.jaws.DocumentBlob;
import org.nuxeo.ecm.platform.api.ws.jaws.DocumentDescriptor;
import org.nuxeo.ecm.platform.api.ws.jaws.DocumentProperty;
import org.nuxeo.ecm.platform.ws.jaws.NuxeoRemotingInterface;
import org.nuxeo.ecm.platform.ws.jaws.NuxeoRemotingInterfaceBindingStub;
import org.nuxeo.ecm.platform.ws.jaws.NuxeoRemotingServiceLocator;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WSClient {

    public static String UUID;

    private NuxeoRemotingInterfaceBindingStub stub;

    // public static WSClient create(String url) {
    // Proxy.newProxyInstance(loader, new Class[] {NuxeoRemoting.class}, )
    // }

    public static NuxeoRemotingInterface create() throws Exception {
        NuxeoRemotingServiceLocator locator = new NuxeoRemotingServiceLocator();
        NuxeoRemotingInterfaceBindingStub _stub = new NuxeoRemotingInterfaceBindingStub(
                new URL(locator.getNuxeoRemotingInterfacePortAddress()),
                locator);
        _stub.setPortName(locator.getNuxeoRemotingInterfacePortWSDDServiceName());
        return _stub;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {

            NuxeoRemotingInterface remoting = create();
            if (remoting == null)
                new Exception("remoting init failed");

            // connection
            String sessionId = remoting.connect("Administrator",
                    "Administrator");

            String repo = remoting.getRepositoryName(sessionId);
            System.out.println("repository: " + repo);

            // user and groups
            String[] groups = remoting.listGroups(sessionId, 0, 1);
            System.out.println("groups: " + Arrays.toString(groups));
            String[] users = remoting.listUsers(sessionId, 0, 1);
            System.out.println("users: " + Arrays.toString(users));

            groups = remoting.getGroups(sessionId, null);
            System.out.println("tope level groups: " + Arrays.toString(groups));
            users = remoting.getUsers(sessionId, groups[0]);
            System.out.println("users in " + groups[0] + ": "
                    + Arrays.toString(users));

            // remoting.listAllDocumentUUIDsAndVersions(string_1, int_1, int_2,
            // boolean_1, string_2);

            // navigation
            DocumentDescriptor doc = remoting.getRootDocument(sessionId);
            printTree(remoting, doc, sessionId, "");

            if (UUID != null) {

                // acl
                ACE[] acl = remoting.getDocumentACL(sessionId, UUID);
                printACL(acl);

                DocumentProperty[] props = remoting.getDocumentProperties(
                        sessionId, UUID);
                printProperties(props);

                props = remoting.getDocumentNoBlobProperties(sessionId, UUID);
                printProperties(props);

                DocumentBlob[] blobs = remoting.getDocumentBlobs(sessionId,
                        UUID);
                printBlobs(blobs);
                //printBlobContents(blobs);

                // rpath
                String rpath = remoting.getRelativePathAsString(sessionId, UUID);
                assert rpath != null;
                assert rpath != "";
                System.out.println("rpath = " + rpath);

            } else {
                System.out.println("No document of type file in the remoting Nuxeo core. Cannot test out ...");
            }

            remoting.disconnect(sessionId);

            System.out.println("done.");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void printTree(NuxeoRemotingInterface remoting,
            DocumentDescriptor parent, String sessionId, String prefix)
            throws Exception {
        System.out.println(prefix + "> " + parent.getTitle() + " ["
                + parent.getId() + " : " + parent.getType() + "] ");
        DocumentDescriptor[] docs = remoting.getChildren(sessionId,
                parent.getId());
        if (docs == null)
            return;
        for (DocumentDescriptor doc : docs) {
            printTree(remoting, doc, sessionId, prefix + "--");
            if (doc.getType().equals("File")) {
                UUID = doc.getId();
            }
        }
    }

    public static void printACL(ACE[] aces) throws Exception {
        for (ACE ace : aces) {
            System.out.println("ACE { " + ace.getUsername() + " : "
                    + ace.getPermission() + " : " + ace.isGranted() + " }");
        }
    }

    public static void printProperties(DocumentProperty[] props)
            throws Exception {
        System.out.println("DOC PROPERTIES: ");
        for (DocumentProperty prop : props) {
            System.out.println(prop.getName() + " = " + prop.getValue());
        }
    }

    public static void printBlobs(DocumentBlob[] blobs) throws Exception {
        System.out.println("DOC BLOBS: ");
        if (blobs == null)
            return;
        for (DocumentBlob blob : blobs) {
            System.out.println(blob.getName() + " { " + blob.getMimeType()
                    + " : " + blob.getEncoding() + "}");
        }
    }

    public static void printBlobContents(DocumentBlob[] blobs) throws Exception {
        for (DocumentBlob blob : blobs) {
            System.out.println(new String(blob.getBlob()));
        }
    }

}
