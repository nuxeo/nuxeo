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
package org.nuxeo.ecm.automation.core.operations;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = RestoreDocumentInput.ID, category = Constants.CAT_EXECUTION, label = "Restore Document Input", description = "Restore the document input from a context variable given its name. Return the document.")
public class RestoreDocumentInput {

    public static final String ID = "Context.RestoreDocumentInput";

    @Context
    protected OperationContext ctx;

    @Param(name = "name")
    protected String name;

    @OperationMethod
    public DocumentModel run() throws Exception {
        Object obj = ctx.get(name);
        if (obj instanceof DocumentModel) {
            return (DocumentModel) obj;
        } else if (obj instanceof DocumentRef) {
            return ctx.getCoreSession().getDocument((DocumentRef) obj);
        }
        throw new OperationException(
                "Illegal state error for restore document operation. The context map doesn't contains a document variable with name "
                        + name);
    }

}
