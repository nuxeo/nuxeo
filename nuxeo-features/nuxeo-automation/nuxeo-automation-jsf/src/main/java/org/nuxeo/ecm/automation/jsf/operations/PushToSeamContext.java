/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 */

package org.nuxeo.ecm.automation.jsf.operations;

import org.jboss.seam.contexts.Contexts;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@Operation(id = PushToSeamContext.ID, category = Constants.CAT_UI, requires = Constants.SEAM_CONTEXT, label = "Push to Seam Context",
        description = "Push the current input document into Seam context. Returns back the document.")
public class PushToSeamContext {

    public static final String ID = "Seam.PushDocument";

    @Context
    protected OperationContext ctx;

    @Param(name = "name")
    protected String name;

    @Param(name = "scope", widget = Constants.W_OPTION, values = { "session",
            "conversation", "page", "event" })
    protected String scope;

    @OperationMethod
    public DocumentModel push(DocumentModel value) {
        if ("session".equalsIgnoreCase(scope)) {
            Contexts.getSessionContext().set(name, value);
        } else if ("conversation".equalsIgnoreCase(scope)) {
            Contexts.getConversationContext().set(name, value);
        } else if ("page".equalsIgnoreCase(scope)) {
            Contexts.getPageContext().set(name, value);
        } else if ("event".equalsIgnoreCase(scope)) {
            Contexts.getEventContext().set(name, value);
        }

        return value;
    }

}
