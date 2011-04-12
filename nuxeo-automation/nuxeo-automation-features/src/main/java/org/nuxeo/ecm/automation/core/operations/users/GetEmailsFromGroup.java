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
import org.nuxeo.ecm.core.api.security.PermissionProvider;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * Retrieve the emails from users/groups who have the given permission on given
 * document.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = GetEmailsFromGroup.ID, category = Constants.CAT_USERS_GROUPS, label = "Get Emails From Group", description =
        "Fetch the emails of the user in a given group. The result will be set into the context variable which key is the one specified in the 'variable' parameter."
        + "If the 'ignoreSubGroups' parameter is true then only direct principal emails (sub-groups will be ignored).<p>This is a void operation and return back the current input object</p>")
public class GetEmailsFromGroup {

    public static final String ID = "Users.GetEmailsFromGroup";

    @Context
    protected PermissionProvider permissionProvider;

    @Context
    protected UserManager umgr;

    @Context
    protected OperationContext ctx;

    @Param(name = "group")
    protected String groupId;

    @Param(name = "variable")
    protected String key;

    @Param(name = "ignoreSubGroups", required = false, values = "false")
    protected boolean ignoreGroups = false;

    @OperationMethod
    public void run() throws Exception {
        PrincipalHelper ph = new PrincipalHelper(umgr, permissionProvider);
        Set<String> emails = ph.getEmailsFromGroup(groupId, !ignoreGroups);

        ctx.put(key, new StringList(emails));
    }

}
