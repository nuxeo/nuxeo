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
package org.nuxeo.ecm.automation.core.scripting;

import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;



/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Functions {

    public final static Functions INSTANCE = new Functions();

    protected volatile DirectoryService dirService = null;
    protected volatile UserManager userMgr;


    public UserManager getUserMgr() throws Exception {
        if (userMgr == null) {
            userMgr = Framework.getService(UserManager.class);
        }
        return userMgr;
    }

    public DirectoryService getDirService() throws Exception {
        if (dirService == null) {
            dirService = Framework.getService(DirectoryService.class);
        }
        return dirService;
    }

    public String getVocabularyLabel(String voc, String key) throws Exception {
        org.nuxeo.ecm.directory.Session session = getDirService().open(voc);
        DocumentModel doc = session.getEntry(key);
        //TODO: which is the best method to get "label" property when not knowning vocabulary schema?
        DataModel dm = doc.getDataModels().values().iterator().next();
        return (String)dm.getData("label");
    }

    public NuxeoPrincipal getPrincipal(String username) throws Exception {
        return getUserMgr().getPrincipal(username);
    }

    public String getEmail(String username) throws Exception {
        NuxeoPrincipal principal = getPrincipal(username);
        UserManager mgr = getUserMgr();
        String key = mgr.getUserEmailField();
        String schema = mgr.getUserSchemaName();
        return (String)principal.getModel().getProperty(schema, key);
    }

//    public String getPrincipalEmail(String username) throws Exception {
//        NuxeoPrincipal principal = getPrincipal(username);
//        UserManager mgr = getUserMgr();
//        String key = mgr.getUserEmailField();
//        String schema = mgr.getUserSchemaName();
//        return (String)principal.getModel().getProperty(schema, key);
//    }

}
