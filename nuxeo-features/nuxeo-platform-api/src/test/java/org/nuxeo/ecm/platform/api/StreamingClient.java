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

package org.nuxeo.ecm.platform.api;

import java.io.File;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.platform.api.login.SystemSession;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;
import org.nuxeo.runtime.services.event.EventService;
import org.nuxeo.runtime.services.streaming.FileSource;
import org.nuxeo.runtime.services.streaming.StreamSource;
import org.nuxeo.runtime.util.NXRuntimeApplication;
import org.nuxeo.runtime.api.Framework;

import javax.security.auth.login.LoginContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class StreamingClient extends NXRuntimeApplication {

    public static void main(String[] args) {
        new StreamingClient().start(args);
    }

    @Override
    protected void deployAll() {
        super.deployAll();
        // we deploy this first to test pending bindings
        deploy("StreamingClient.xml");

        deploy("PlatformService.xml");
        deploy("DefaultPlatform.xml");
    }

    @Override
    protected void run() throws Exception {
        LoginContext loginContext = Framework.login();

        // upload a big blob
        Platform platform = ECM.getPlatform();
        Repository repository = platform.getDefaultRepository();
        System.out.println("Connecting to: " + repository.getName());
        CoreSession session = repository.open();

        String name = "__test-file-" + System.currentTimeMillis();
        DocumentModel doc = session.getDocument(new IdRef("dc453407-52cb-4ec9-aecb-479dfabbf68f"));
        DocumentModel childFile = new DocumentModelImpl(doc.getPathAsString(),
                name, "File");
        childFile = session.createDocument(childFile);

        session.save();

        StreamSource src = new FileSource(new File("/home/bstefanescu/kits/ee"));
        Blob blob = new StreamingBlob(src, "audio/mpeg");

        childFile.setProperty("file", "filename", "test.blob");
        childFile.setProperty("dublincore", "title", "TEST_TEST");
        childFile.setProperty("dublincore", "description", "desc 1");
        childFile.setProperty("file", "content", blob);

        session.saveDocument(childFile);
        session.save();

        CoreInstance.getInstance().close(session);

        loginContext.logout();
    }

    protected void repositoryExample(Platform platform) throws Exception {
        // use the first declared repository
        Repository repository = platform.getDefaultRepository();
        System.out.println("Connecting to: " + repository.getName());
        CoreSession session = platform.openRepository(repository.getName());

        DocumentModel root = session.getRootDocument();
        System.out.println(">>> root: " + root.getPathAsString());

        CoreInstance.getInstance().close(session);
    }

    protected void serviceExample(Platform platform) throws Exception {
//        TypeManager typeMgr = platform.getService(TypeManager.class);
//        System.out.println(">>> ECM Types: "+typeMgr.getTypes());
    }

    protected void localServiceTest(Platform platform) throws Exception {
        EventService eventService = platform.getService(EventService.class);
        eventService.addListener("test", new EventListener() {
            public boolean aboutToHandleEvent(Event event) {
                return false;
            }
            public void handleEvent(Event event) {
                System.out.println("Received event:" + event);
            }
        });

        eventService.sendEvent(new Event("test", "my event", this, null));
    }

}
