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
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * Generic fetch document operation that can be used on any context that has a
 * document as the input. This operation is taking the context input and it is
 * returning it as a document If the input is not a document an exception is
 * thrown
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = FetchContextDocument.ID, category = Constants.CAT_FETCH, label = "Context Document", description = "Fetch the input of the context as a document. The document will become the input for the next operation.")
public class FetchContextDocument {

    public static final String ID = "Context.FetchDocument";

    @Context
    protected OperationContext ctx;

    @OperationMethod
    public DocumentModel run() throws Exception {
        Object input = ctx.getInput();
        if (input instanceof DocumentModel) {
            return (DocumentModel) input;
        } else if (input instanceof DocumentRef) {
            return ctx.getCoreSession().getDocument((DocumentRef) input);
        }
        throw new OperationException(
                "Unsupported context for FetchDocument operation. No document available as input in the context");
    }

}
