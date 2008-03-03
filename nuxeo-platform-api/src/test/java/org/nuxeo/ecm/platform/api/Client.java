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

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.platform.api.login.SystemSession;
import org.nuxeo.ecm.platform.api.login.UserSession;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;
import org.nuxeo.runtime.services.event.EventService;
import org.nuxeo.runtime.util.NXRuntimeApplication;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Client extends NXRuntimeApplication {

    public static void main(String[] args) {
        new Client().start(args);
    }

    @Override
    protected void deployAll() {
        super.deployAll();
        deploy("ServiceManagement.xml");
        deploy("LoginComponent.xml");
        deploy("JBossLoginConfig.xml");
        deploy("RepositoryManager.xml");

        // we deploy this first to test pending bindings
        //deploy("TypeManagerBinding.xml");

        deploy("PlatformService.xml");
        deploy("DefaultPlatform.xml");
    }

    @Override
    protected void run() throws Exception {
        Platform platform;
        UserSession us = new UserSession("Administrator", "Administrator");
        us.login();
        // ------------ user session started -----------
        platform = ECM.getPlatform();
        repositoryExample(platform);
        serviceExample(platform);
        localServiceTest(platform);
        // ---------------------------------------------------
        us.logout();

        SystemSession ss = new SystemSession();
        ss.login();
        // ------------ system session started -----------
        platform = ECM.getPlatform();
        repositoryExample(platform);
        serviceExample(platform);
        localServiceTest(platform);
        // ---------------------------------------------------
        ss.logout();
    }

    protected void repositoryExample(Platform platform) throws Exception {
        // use the first declared repository
        Repository repository = platform.getDefaultRepository();
        System.out.println("Connecting to: " + repository.getName());
        CoreSession session = platform.openRepository(repository.getName());

        DocumentModel root = session.getRootDocument();
        System.out.println(">>> root: " + root.getPathAsString());

        session.getChildren(root.getRef());

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
