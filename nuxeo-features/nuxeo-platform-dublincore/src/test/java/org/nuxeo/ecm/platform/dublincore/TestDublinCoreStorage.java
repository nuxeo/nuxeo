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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.dublincore;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.listener.CoreEventListenerService;
import org.nuxeo.ecm.core.listener.EventListener;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.ecm.platform.dublincore.service.DublinCoreStorageService;

/**
 * DublinCoreStorage Test Case.
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
public class TestDublinCoreStorage extends RepositoryTestCase {

    private static final Log log = LogFactory.getLog(DublinCoreListener.class);

    private DocumentModel root;

    private CoreSession remote;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        deployContrib("org.nuxeo.ecm.platform.dublincore.tests",
                "CoreService.xml");
        deployContrib("org.nuxeo.ecm.platform.dublincore.tests",
                "test-CoreExtensions.xml");
        deployContrib("org.nuxeo.ecm.platform.dublincore.tests",
                "DemoRepository.xml");
        deployContrib("org.nuxeo.ecm.platform.dublincore.tests",
                "CoreEventListenerService.xml");
        deployContrib("org.nuxeo.ecm.platform.dublincore.tests",
                "LifeCycleService.xml");

        deployContrib("org.nuxeo.ecm.platform.dublincore.tests",
                "nxdublincore-bundle.xml");

        Map<String, Serializable> context = new HashMap<String, Serializable>();
        context.put("username", "Administrator");
        remote = CoreInstance.getInstance().open("demo", context);
        assertNotNull(remote);

        root = remote.getRootDocument();
    }

    private static CoreEventListenerService getListenerService() {
        return NXCore.getCoreEventListenerService();
    }

    public void testServiceRegistration() {
        CoreEventListenerService listenerService = getListenerService();
        EventListener dcListener = listenerService.getEventListenerByName("dclistener");
        assertNotNull(dcListener);
        log.info("DCListener well registered");
    }

    public void testStorageService() {
        DublinCoreStorageService service = NXDublinCore.getDublinCoreStorageService();
        assertNotNull(service);
    }

    public void testCreationDate() throws DocumentException, ClientException {
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                "file-007", "File");
        DocumentModel childFile2 = remote.createDocument(childFile);

        DataModel dm;
        dm = childFile2.getDataModel("dublincore");
        assertNotNull(dm.getData("created"));

        dm = remote.getDataModel(childFile2.getRef(), "dublincore");
        assertNotNull(dm.getData("created"));

        // assertEquals("toto", (String)dm.getData("creator"));
    }

    public void testModificationDate() throws DocumentException,
            ClientException {
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                "file-008", "File");
        DocumentModel childFile2 = remote.createDocument(childFile);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }

        childFile2.setProperty("dublincore", "title", "toto");

        remote.saveDocument(childFile2);

        DataModel dm;
        dm = childFile2.getDataModel("dublincore");
        Calendar created = (Calendar) dm.getData("created");
        assertNotNull(created);

        dm = remote.getDataModel(childFile2.getRef(), "dublincore");
        Calendar modified = (Calendar) dm.getData("modified");
        assertNotNull(modified);

        assertTrue(modified.getTime() + " !> " +created.getTime(), modified.after(created));
    }

    // Wait until we can have a real list management
    public void testContributors() throws DocumentException, ClientException {
        DocumentModel childFile = new DocumentModelImpl(root.getPathAsString(),
                "file-008", "File");
        DocumentModel childFile2 = remote.createDocument(childFile);
        DataModel dm = childFile2.getDataModel("dublincore");

        String[] contributorsArray = (String[]) dm.getData("contributors");

        List<String> contributorsList = Arrays.asList(contributorsArray);
        assertTrue(contributorsList.contains("Administrator"));

        String author = (String) dm.getData("creator");
        assertEquals("Administrator", author);

        // modify security to test with a new user

        ACP acp = root.getACP();
        ACL[] acls = acp.getACLs();
        ACL theAcl = acls[0];
        ACE ace = new ACE("Jacky", SecurityConstants.EVERYTHING, true);
        theAcl.add(ace);
        root.setACP(acp, true);

        // create a new session
        remote.save();
        remote.disconnect();
        remote = null;

        Map<String, Serializable> context = new HashMap<String, Serializable>();
        // UserPrincipal newUser = new UserPrincipal("Jacky");
        // newUser.groups.add(SecurityService.ADMINISTRATORS);
        // context.put("username", newUser);
        // switch user in session
        // LocalSession local = (LocalSession) remote;
        // local.setPrincipal(newUser);
        context.put("username", "Jacky");
        remote = CoreInstance.getInstance().open("demo", context);

        DocumentModel childFile3 = remote.getDocument(childFile2.getRef());
        childFile3.setProperty("dublincore", "source", "testing");
        childFile3 = remote.saveDocument(childFile3);

        contributorsArray = (String[]) childFile3.getDataModel("dublincore").getData(
                "contributors");
        contributorsList = Arrays.asList(contributorsArray);
        assertTrue(contributorsList.contains("Jacky"));
    }

}
