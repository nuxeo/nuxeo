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
 *     dmetzler
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 *     Mincong Huang <mhuang@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.document;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.PermissionProvider;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.model.exceptions.IllegalParameterException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Operation that adds a permission to a given ACL for a given user.
 *
 * @since 5.7.3
 */
@Operation(id = AddPermission.ID, category = Constants.CAT_DOCUMENT, label = "Add Permission", description = "Add Permission on the input document(s). Returns the document(s).", aliases = { "Document.AddACL" })
public class AddPermission {

    public static final String ID = "Document.AddPermission";

    public static final String NOTIFY_KEY = "notify";

    public static final String COMMENT_KEY = "comment";

    /**
     * Configuration property name, which defines whether virtual user (non-existent user) is allowed in Nuxeo
     * automation. If allowed, Nuxeo server will not check the user existence during automation execution. Set this
     * property to true if you use Nuxeo computed user or computed group.
     */
    public static final String ALLOW_VIRTUAL_USER = "nuxeo.automation.allowVirtualUser";

    @Context
    protected CoreSession session;

    @Param(name = "username", required = false, alias = "user", description = "ACE target user/group.")
    protected String user;

    /**
     * @since 8.1
     */
    @Param(name = "email", required = false, description = "ACE target user/group.")
    protected String email;

    @Param(name = "permission", description = "ACE permission.")
    String permission;

    @Param(name = "acl", required = false, values = { ACL.LOCAL_ACL }, description = "ACL name.")
    String aclName = ACL.LOCAL_ACL;

    @Param(name = "begin", required = false, description = "ACE begin date.")
    Calendar begin;

    @Param(name = "end", required = false, description = "ACE end date.")
    Calendar end;

    @Param(name = "blockInheritance", required = false, description = "Block inheritance or not.")
    boolean blockInheritance = false;

    @Param(name = "notify", required = false, description = "Notify the user or not")
    boolean notify = false;

    @Param(name = "comment", required = false, description = "Comment")
    String comment;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) {
        validateParameters();
        addPermission(doc);
        return session.getDocument(doc.getRef());
    }

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentRef docRef) {
        DocumentModel doc = session.getDocument(docRef);
        validateParameters();
        addPermission(doc);
        return doc;
    }

    protected void validateParameters() {
        // validate permission
        if (!Arrays.asList(Framework.getService(PermissionProvider.class).getPermissions()).contains(permission)) {
            throw new IllegalParameterException(String.format("Permission %s is invalid.", permission));
        }
    }

    protected void addPermission(DocumentModel doc) {
        if (user == null && email == null) {
            throw new IllegalParameterException("'username' or 'email' parameter must be set");
        }

        if (user == null && end == null) {
            throw new IllegalParameterException("'end' parameter must be set when adding a permission for an 'email'");
        }

        String username;
        if (user == null) {
            // share a document with someone not registered in Nuxeo, by using only an email
            username = NuxeoPrincipal.computeTransientUsername(email);
        } else {
            username = user;
            ConfigurationService configService = Framework.getService(ConfigurationService.class);
            if (configService.isBooleanPropertyFalse(ALLOW_VIRTUAL_USER)) {
                checkUserExistence(username);
            }
        }

        ACP acp = doc.getACP() != null ? doc.getACP() : new ACPImpl();
        Map<String, Serializable> contextData = new HashMap<>();
        contextData.put(NOTIFY_KEY, notify);
        contextData.put(COMMENT_KEY, comment);

        String creator = session.getPrincipal().getName();
        ACE ace = ACE.builder(username, permission)
                     .creator(creator)
                     .begin(begin)
                     .end(end)
                     .contextData(contextData)
                     .build();
        boolean permissionChanged = false;
        if (blockInheritance) {
            permissionChanged = acp.blockInheritance(aclName, creator);
        }
        permissionChanged = acp.addACE(aclName, ace) || permissionChanged;
        if (permissionChanged) {
            doc.setACP(acp, true);
        }
    }

    protected void checkUserExistence(String username) {
        UserManager userManager = Framework.getService(UserManager.class);
        if (userManager.getUserModel(username) == null && userManager.getGroupModel(username) == null) {
            String errorMsg = "User or group name '" + username + "' does not exist. Please provide a valid name.";
            throw new NuxeoException(errorMsg);
        }
    }

}
