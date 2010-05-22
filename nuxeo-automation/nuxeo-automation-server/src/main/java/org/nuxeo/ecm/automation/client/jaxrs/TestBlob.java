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
package org.nuxeo.ecm.automation.client.jaxrs;

import java.io.File;

import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.jaxrs.model.Blob;
import org.nuxeo.ecm.automation.client.jaxrs.model.FileBlob;
import org.nuxeo.ecm.automation.core.operations.blob.AttachBlob;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TestBlob {

    public static void main(String[] args) throws Exception {
        try {
            HttpAutomationClient client = new HttpAutomationClient();
            client.connect("http://localhost:8080/automation");
            long start = System.currentTimeMillis();
            Session session = client.getSession("Administrator", "Administrator");
            //FileBlob fb = new FileBlob(new File("/Users/bstefanescu/operations.jpg"));
            FileBlob fb = new FileBlob(new File("/Users/bstefanescu/test.jpg"));
            fb.setMimeType("image/jpeg");
            Blob blob = (Blob)session.newRequest(AttachBlob.ID).setInput(fb).set("document", "/default-domain/workspaces/myws/file").execute(); //TODO avoid getting the output from server
            System.out.println(((FileBlob)blob).getFile());
            System.out.println("took: "+((double)System.currentTimeMillis()-start)/1000);
            client.shutdown();
        } catch (RemoteException e) {
            e.printStackTrace();
            System.out.println(e.getRemoteStackTrace());
        }
    }

}
