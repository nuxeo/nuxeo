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

package org.nuxeo.ecm.platform.filemanager;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Properties;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public abstract class TestFake extends NXRuntimeTestCase {

    protected CoreSession remote;

    protected Properties properties;

    protected Subject authenticatedSubject;

    protected LoginContext loginContext;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deploy("CoreService.xml");
        deploy("TypeService.xml");
        deploy("SecurityService.xml");
        deploy("RepositoryService.xml");
        deploy("test-CoreExtensions.xml");
        deploy("LifeCycleService.xml");
        deploy("CoreTestExtensions.xml");
        deploy("DemoRepository.xml");
        deploy("nxmimetype-bundle.xml");
        deploy("nxfilemanag-bundle-points.xml");
        deploy("nxfilemanag-bundle.xml");
        CoreInstance instance =  CoreInstance.getInstance();
        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        ctx.put("username", SecurityConstants.ADMINISTRATOR);
        remote = instance.open("demo", ctx);
        assertNotNull(remote);
    }

    @Override
    public void tearDown() throws Exception {
        DocumentModel root = remote.getRootDocument();
        DocumentModelList docList =  remote.getChildren(root.getRef());
        for(DocumentModel child : docList){
            String type = child.getType();
            if (type.equals("File") || type.equals("Folder")){
                remote.removeDocument(child.getRef());
            }
        }

        remote.save();

        properties = null;
        authenticatedSubject = null;
        loginContext = null;
        super.tearDown();
    }

}
