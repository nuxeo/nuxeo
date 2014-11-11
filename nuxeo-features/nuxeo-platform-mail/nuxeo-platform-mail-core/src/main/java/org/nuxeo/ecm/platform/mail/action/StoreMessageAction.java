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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.mail.action;

import java.util.Map;

import javax.mail.Message;
import javax.security.auth.login.LoginContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Alexandre Russel
 */
public class StoreMessageAction implements MessageAction {

    public static final String MAIL_MESSAGE = "MailMessage";
    private static final Log log = LogFactory.getLog(StoreMessageAction.class);

    protected final String parentPath;

    public StoreMessageAction(String parentPath) {
        this.parentPath = parentPath;
    }

    @SuppressWarnings("unchecked")
    public boolean execute(ExecutionContext context) throws Exception {
        PathSegmentService pss = Framework.getService(PathSegmentService.class);
        Message message = context.getMessage();
        String title = message.getSubject();
        if (log.isDebugEnabled()) {
            log.debug("Storing message: " + message.getSubject());
        }
        Thread.currentThread().setContextClassLoader(Framework.class.getClassLoader());
        LoginContext login = Framework.login();
        CoreInstance server = CoreInstance.getInstance();
        CoreSession session = server.open("default", null);
        DocumentModel doc = session.createDocumentModel(getMailDocumentType());
        doc.setProperty("dublincore", "title",
                title + System.currentTimeMillis());
        doc.setPathInfo(parentPath, pss.generatePathSegment(doc));
        doc.setProperty("dublincore", "title", title);
        doc = session.createDocument(doc);
        Map<String, Map<String, Object>> schemas = (Map<String, Map<String, Object>>) context.get(
                "transformed");
        for (Map.Entry<String, Map<String, Object>> entry : schemas.entrySet()) {
            doc.setProperties(entry.getKey(), entry.getValue());
        }
        doc = session.saveDocument(doc);
        ACL acl = (ACL) context.get("acl");
        if(acl != null) {
            ACP acp = doc.getACP();
            acp.addACL(0, acl);
            doc.setACP(acp, true);
        }
        session.save();
        context.put("document", doc);
        login.logout();
        return true;
    }

    protected String getMailDocumentType() {
        return MAIL_MESSAGE;
    }

    public void reset(ExecutionContext context) throws Exception {
        //do nothing
    }

}
