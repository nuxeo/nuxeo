/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stéphane Fourrier
 */

package org.nuxeo.opensocial.container.dev;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.dev.NuxeoApp;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.webengine.gwt.dev.NuxeoLauncher;
import org.nuxeo.runtime.api.Framework;

public class OpenSocialNuxeoLauncher extends NuxeoLauncher {

    @Override
    protected void aboutToStartFramework(NuxeoApp app) {
        super.aboutToStartFramework(app);
        copyConfig(app, "opensocial.properties");
        copyConfig(app, "default-opensocial-config.xml");
    }

    private void copyConfig(NuxeoApp app, String fileName) {
        File config = new File(app.getHome(), "config");
        if (!config.exists()) {
            throw new RuntimeException("Config dir is not present");
        }

        URL url = OpenSocialNuxeoLauncher.class.getResource(fileName);
        if (url == null) {
            throw new IllegalArgumentException("properties file is not known");
        }

        try {
            FileUtils.copyFile(new File(url.getFile()), config);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    protected void frameworkStarted(NuxeoApp app) {

        // In order to avoid security problems we allow everything to "Invite"
        try {

            Map<String, Serializable> user = new HashMap<String, Serializable>();
            user.put("username", "Administrator");
            user.put("password", "Administrator");

            RepositoryManager m = Framework.getService(RepositoryManager.class);
            CoreSession session = m.getRepository("default").open(user);
            DocumentModel doc = session.getDocument(new PathRef("/"));
            ACPImpl acp = new ACPImpl();
            ACLImpl acl = new ACLImpl(ACL.LOCAL_ACL);
            acp.addACL(acl);
            ACE ace = new ACE("Invite", SecurityConstants.EVERYTHING, true);
            acl.add(ace);
            session.setACP(doc.getRef(), acp, false);
            session.save();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
