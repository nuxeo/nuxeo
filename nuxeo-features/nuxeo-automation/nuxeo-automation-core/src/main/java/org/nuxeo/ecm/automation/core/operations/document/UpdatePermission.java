/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.document;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;

/**
 * Updates a given ACE.
 * <p>
 * Updates only the non-null fields, otherwise keep the ones of the old ACE.
 *
 * @since 7.4
 */
@Operation(id = UpdatePermission.ID, category = Constants.CAT_DOCUMENT, label = "Add Permission", description = "Update a given permission on the input document(s). Returns the document(s).")
public class UpdatePermission {

    public static final String ID = "Document.UpdatePermission";

    public static final String NOTIFY_KEY = "notify";

    public static final String COMMENT_KEY = "comment";

    @Context
    protected CoreSession session;

    @Param(name = "username", alias = "user", description = "ACE target user/group.")
    protected String user;

    @Param(name = "permission", description = "ACE permission.")
    String permission;

    @Param(name = "acl", required = false, values = { ACL.LOCAL_ACL }, description = "ACL name.")
    String aclName = ACL.LOCAL_ACL;

    @Param(name = "begin", required = false, description = "ACE begin date.")
    Calendar begin;

    @Param(name = "end", required = false, description = "ACE end date.")
    Calendar end;

    @Param(name = "id", description = "ACE id.")
    String id;

    @Param(name = "notify", required = false, description = "Notify the user or not")
    Boolean notify;

    @Param(name = "comment", required = false, description = "Comment")
    String comment;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) throws ClientException {
        updatePermission(doc);
        return session.getDocument(doc.getRef());
    }

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentRef docRef) throws ClientException {
        DocumentModel doc = session.getDocument(docRef);
        updatePermission(doc);
        return doc;
    }

    protected void updatePermission(DocumentModel doc) throws ClientException {
        Map<String, Serializable> contextData = new HashMap<>();
        if (notify != null && notify) {
            contextData.put(NOTIFY_KEY, true);
            if (comment != null) {
                contextData.put(COMMENT_KEY, comment);
            }
        }

        ACE oldACE = ACE.fromId(id);
        String username = user != null ? user : oldACE.getUsername();
        String permission = this.permission != null ? this.permission : oldACE.getPermission();
        String creator = session.getPrincipal().getName();

        ACE newACE = ACE.builder(username, permission)
                        .creator(creator)
                        .begin(begin != null ? begin : oldACE.getBegin())
                        .end(end != null ? end : oldACE.getEnd())
                        .contextData(contextData)
                        .build();

        session.replaceACE(doc.getRef(), aclName, oldACE, newACE);
    }

}
