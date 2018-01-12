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

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = Logout.ID, category = Constants.CAT_USERS_GROUPS, label = "Logout", description = "Perform a logout. This should be used only after using the Login As operation to restore original login. This is a void operations - the input will be returned back as the output.")
public class Logout {

    public static final String ID = "Auth.Logout";

    @Context
    protected OperationContext ctx;

    @OperationMethod
    public void run() {
        ctx.getLoginStack().pop();
    }

    @OperationMethod
    public DocumentModel run(DocumentModel doc) {
        run();
        // refetch the input document if any using the new session
        // otherwise using document methods that are delegating the call to the
        // session that created the document will call the old session.
        return ctx.getCoreSession().getDocument(doc.getRef());
    }

}
