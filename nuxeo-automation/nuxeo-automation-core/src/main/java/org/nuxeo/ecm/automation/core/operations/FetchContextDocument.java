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

    public final static String ID = "Context.FetchDocument";

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
