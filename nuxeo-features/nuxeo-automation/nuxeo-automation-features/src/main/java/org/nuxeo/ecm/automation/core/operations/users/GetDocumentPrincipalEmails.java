/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.operations.users;

import java.util.Set;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.automation.features.PrincipalHelper;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.PermissionProvider;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * Retrieve the emails from users/groups who have the given permission on given document.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = GetDocumentPrincipalEmails.ID, category = Constants.CAT_USERS_GROUPS, label = "Get Principal Emails", description = ""
        + "Fetch the principal emails that have a given permission on the input "
        + "document and then set them in the context under the given key variable "
        + "name. The operation returns the input document. You can later use the "
        + "list of principals set by this operation on the context from another "
        + "operation. The 'key' argument represents the variable name and the "
        + "'permission' argument the permission to check. If the 'ignore groups' "
        + "argument is false then groups are recursively resolved, extracting "
        + "user members of these groups. Be <b>warned</b> "
        + "that this may be a very consuming operation.<ul>Note that <li></li>"
        + "<li>groups are not included</li><li>the list pushed into the context "
        + "is a string list of emails.</li></ul>", aliases = { "Document.GetPrincipalEmails" })
public class GetDocumentPrincipalEmails {

    public static final String ID = "Context.GetEmailsWithPermissionOnDoc";

    @Context
    protected PermissionProvider permissionProvider;

    @Context
    protected UserManager umgr;

    @Context
    protected OperationContext ctx;

    @Param(name = "permission")
    protected String permission;

    @Param(name = "variable name")
    protected String key;

    @Param(name = "ignore groups", required = false, values = { "false" })
    protected boolean ignoreGroups = false;

    @OperationMethod
    public DocumentModel run(DocumentModel input) {
        PrincipalHelper ph = new PrincipalHelper(umgr, permissionProvider);
        Set<String> result = ph.getEmailsForPermission(input, permission, ignoreGroups);
        ctx.put(key, new StringList(result));
        return input;
    }

}
