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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
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
public class Functions {

    public static final Functions INSTANCE = new Functions();

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
        DocumentModel doc = session.getEntry(key);
        // TODO: which is the best method to get "label" property when not
        // knowing vocabulary schema?
        // AT: the best is to accept it as a parameter of the method, and
        // fallback on "label" when not given
        DataModel dm = doc.getDataModels().values().iterator().next();
        return (String) dm.getData("label");
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

    public DateWrapper date(Date date) {
        return new DateWrapper(date);
    }

    public DateWrapper calendar(Calendar date) {
        return new DateWrapper(date);
    }

    public String escapeHtml(Object obj) {
        return StringEscapeUtils.escapeHtml(obj.toString());
    }
}
