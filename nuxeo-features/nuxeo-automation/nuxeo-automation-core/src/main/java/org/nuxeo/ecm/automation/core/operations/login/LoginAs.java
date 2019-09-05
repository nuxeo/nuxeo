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
package org.nuxeo.ecm.automation.core.operations.login;

import java.security.Principal;

import javax.security.auth.login.LoginException;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.NuxeoLoginContext;

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
    public void run() throws LoginException, OperationException {
        NuxeoLoginContext lc;
        if (name == null) {
            Principal origPrincipal = ctx.getPrincipal();
            lc = Framework.loginSystem(origPrincipal.getName());
        } else {
            lc = Framework.loginUser(name);
        }
        ctx.getLoginStack().push(lc);
    }

    @OperationMethod
    public DocumentModel run(DocumentModel doc) throws LoginException, OperationException {
        run();
        // refetch the input document if any using the new session
        // otherwise using document methods that are delegating the call to the
        // session that created the document will call the old session.
        return ctx.getCoreSession().getDocument(doc.getRef());
    }
}
