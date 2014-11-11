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
package org.nuxeo.ecm.automation.core.operations.login;

import java.security.Principal;

import javax.security.auth.login.LoginContext;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
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
            lc = Framework.loginAsUser(name);
        }
        if (lc != null) {
            ctx.getLoginStack().push(lc);
        }
    }

}
