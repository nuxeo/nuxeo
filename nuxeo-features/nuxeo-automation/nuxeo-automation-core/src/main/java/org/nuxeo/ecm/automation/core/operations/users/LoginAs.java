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
package org.nuxeo.ecm.automation.core.operations.users;

import java.security.Principal;

import javax.security.auth.login.LoginContext;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = LoginAs.ID, category = Constants.CAT_USERS_GROUPS, label = "Login As", description = "Login As the given user. If no user is given a system login is performed. This is a void operations - the input will be returned back as the output.")
public class LoginAs {

    public static final String ID = "Auth.LoginAs";

    @Context
    protected OperationContext ctx;

    @Param(name = "name", required = false)
    protected String name;

    @OperationMethod
    public void run() throws Exception {
        LoginContext lc = null;
        if (name == null) {
            Principal origPrincipal = ctx.getPrincipal();
            if (origPrincipal != null) {
                lc = Framework.loginAs(origPrincipal.getName());
            } else {
                lc = Framework.login();
            }
        } else {
            lc = NuxeoAuthenticationFilter.loginAs(name);
        }
        if (lc != null) {
            ctx.getLoginStack().push(lc);
        }
    }

}
