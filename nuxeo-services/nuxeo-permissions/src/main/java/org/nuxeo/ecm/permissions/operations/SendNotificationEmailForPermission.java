/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.permissions.operations;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.permissions.PermissionHelper;

/**
 * Operation that sends the notification email for a given {@code ACE}.
 *
 * @since 8.1
 */
@Operation(id = SendNotificationEmailForPermission.ID, category = Constants.CAT_DOCUMENT, label = "Send the notification email for a permission", description = "Send the notification email for a permission on the input document(s). Returns the document(s).")
public class SendNotificationEmailForPermission {

    public static final String ID = "Document.SendNotificationEmailForPermission";

    @Context
    protected CoreSession session;

    @Param(name = "id", description = "ACE id.")
    String id;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) {
        sendEmail(doc);
        return session.getDocument(doc.getRef());
    }

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentRef docRef) {
        DocumentModel doc = session.getDocument(docRef);
        sendEmail(doc);
        return doc;
    }

    protected void sendEmail(DocumentModel doc) {
        ACE ace = ACE.fromId(id);

        // check that the ACE exists
        String aclName = null;
        ACP acp = doc.getACP();
        for (ACL acl : acp.getACLs()) {
            if (acl.contains(ace)) {
                aclName = acl.getName();
                break;
            }
        }

        if (aclName == null) {
            // ACE was not found
            return;
        }

        PermissionHelper.firePermissionNotificationEvent(session, doc, aclName, ace);
    }
}
