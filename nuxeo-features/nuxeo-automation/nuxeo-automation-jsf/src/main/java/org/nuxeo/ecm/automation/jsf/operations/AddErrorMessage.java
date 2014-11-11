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

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;

/**
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@Operation(id = AddErrorMessage.ID, category = Constants.CAT_UI, requires = Constants.SEAM_CONTEXT, label = "Add Error Message", description = "Add a message to be displayed in case the chain execution fails. This is a void operation - the input will be returned back as output")
public class AddErrorMessage {

    public static final String ID = "Seam.AddErrorMessage";

    @Context
    protected OperationContext ctx;

    @Param(name = "message")
    protected String message;

    @OperationMethod
    public void run() {
        ctx.put(ID, message);
    }

}
