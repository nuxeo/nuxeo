/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.features;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.automation.core.scripting.CoreFunctions;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.uidgen.UIDSequencer;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class PlatformFunctions extends CoreFunctions {


    protected volatile DirectoryService dirService;

    protected volatile UserManager userMgr;

    public UserManager getUserManager() throws Exception {
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
        try {
            DocumentModel doc = session.getEntry(key);
            // TODO: which is the best method to get "label" property when not
            // knowing vocabulary schema?
            // AT: the best is to accept it as a parameter of the method, and
            // fallback on "label" when not given
            DataModel dm = doc.getDataModels().values().iterator().next();
            return (String) dm.getData("label");
        } finally {
            session.close();
        }
    }

    public NuxeoPrincipal getPrincipal(String username) throws Exception {
        return getUserManager().getPrincipal(username);
    }

    protected String getEmail(NuxeoPrincipal principal, String userSchemaName,
            String userEmailFieldName) throws ClientException {
        if (principal == null) {
            return null;
        }
        return (String) principal.getModel().getProperty(userSchemaName,
                userEmailFieldName);
    }

    public String getEmail(String username) throws Exception {
        UserManager userManager = getUserManager();
        return getEmail(userManager.getPrincipal(username),
                userManager.getUserSchemaName(),
                userManager.getUserEmailField());
    }

    public StringList getPrincipalEmails(List<NuxeoPrincipal> principals)
            throws Exception {
        StringList result = new StringList(principals.size());
        String schemaName = userMgr.getUserSchemaName();
        String fieldName = userMgr.getUserEmailField();
        for (NuxeoPrincipal principal : principals) {
            String email = getEmail(principal, schemaName, fieldName);
            if (!StringUtils.isEmpty(email)) {
                result.add(email);
            }
        }
        return result;
    }

    public StringList getEmails(List<String> usernames) throws Exception {
        UserManager userManager = getUserManager();
        StringList result = new StringList(usernames.size());
        String schemaName = userMgr.getUserSchemaName();
        String fieldName = userMgr.getUserEmailField();
        for (String username : usernames) {
            NuxeoPrincipal principal = userManager.getPrincipal(username);
            if (principal != null) {
                String email = getEmail(principal, schemaName, fieldName);
                if (!StringUtils.isEmpty(email)) {
                    result.add(email);
                }
            }
        }
        return result;
    }

    public String getNextId(final String key) throws Exception {
        UIDSequencer svc = Framework.getService(UIDSequencer.class);
        return Integer.toString(svc.getNext(key));
    }

}
