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
package org.nuxeo.ecm.automation.core.operations.stack;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * Pop a document from the context stack and restore the input from the poped document. If on the top of the stack there
 * is no document an exception is thrown This operation contains dynamic logic so it should be handled in a special way
 * by the UI tools to validate the chain (a Pop operation can succeed only if the last push operation has the same type
 * as the pop)
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = PopDocument.ID, category = Constants.CAT_EXECUTION_STACK, label = "Pop Document", description = "Restore the last saved input document in the context input stack. This operation must be used only if a PUSH operation was previously made. Return the last <i>pushed</i> document.", aliases = { "Document.Pop" })
public class PopDocument {

    public static final String ID = "Context.PopDocument";

    @Context
    protected OperationContext ctx;

    @OperationMethod
    public DocumentModel run() throws OperationException {
        Object obj = ctx.pop(Constants.O_DOCUMENT);
        if (obj instanceof DocumentModel) {
            return (DocumentModel) obj;
        } else if (obj instanceof DocumentRef) {
            return ctx.getCoreSession().getDocument((DocumentRef) obj);
        }
        throw new OperationException(
                "Illegal state error for pop document operation. The context stack doesn't contains a document on its top");
    }

}
