/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.mail.action;

import java.util.Map;

import javax.mail.Message;
import javax.mail.MessagingException;

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

    @Override
    @SuppressWarnings("unchecked")
    public boolean execute(ExecutionContext context) throws MessagingException {
        PathSegmentService pss = Framework.getService(PathSegmentService.class);
        Message message = context.getMessage();
        String title = message.getSubject();
        if (log.isDebugEnabled()) {
            log.debug("Storing message: " + message.getSubject());
        }
        Thread.currentThread().setContextClassLoader(Framework.class.getClassLoader());
        CoreSession session = CoreInstance.getCoreSessionSystem(null);
        DocumentModel doc = session.createDocumentModel(getMailDocumentType());
        doc.setProperty("dublincore", "title", title + System.currentTimeMillis());
        doc.setPathInfo(parentPath, pss.generatePathSegment(doc));
        doc.setProperty("dublincore", "title", title);
        doc = session.createDocument(doc);
        Map<String, Map<String, Object>> schemas = (Map<String, Map<String, Object>>) context.get("transformed");
        for (Map.Entry<String, Map<String, Object>> entry : schemas.entrySet()) {
            doc.setProperties(entry.getKey(), entry.getValue());
        }
        doc = session.saveDocument(doc);
        ACL acl = (ACL) context.get("acl");
        if (acl != null) {
            ACP acp = doc.getACP();
            acp.addACL(acl);
            doc.setACP(acp, true);
        }
        session.save();
        context.put("document", doc);
        return true;
    }

    protected String getMailDocumentType() {
        return MAIL_MESSAGE;
    }

    @Override
    public void reset(ExecutionContext context) {
        // do nothing
    }

}
