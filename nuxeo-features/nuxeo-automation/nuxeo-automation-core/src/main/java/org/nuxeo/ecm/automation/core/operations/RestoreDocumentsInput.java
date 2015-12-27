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
package org.nuxeo.ecm.automation.core.operations;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentRefList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = RestoreDocumentsInput.ID, category = Constants.CAT_EXECUTION, label = "Restore Documents Input", description = "Restore the document list input from a context variable given its name. Return the document list.")
public class RestoreDocumentsInput {

    public static final String ID = "Context.RestoreDocumentsInput";

    @Context
    protected OperationContext ctx;

    @Param(name = "name")
    protected String name;

    @OperationMethod
    public DocumentModelList run() throws OperationException {
        Object obj = ctx.get(name);
        if (obj instanceof DocumentModelList) {
            return (DocumentModelList) obj;
        } else if (obj instanceof DocumentRefList) {
            CoreSession session = ctx.getCoreSession();
            DocumentRefList refs = (DocumentRefList) obj;
            DocumentModelListImpl list = new DocumentModelListImpl((int) refs.totalSize());
            for (DocumentRef ref : refs) {
                list.add(session.getDocument(ref));
            }
            // FIXME: variable list is never used!
        }
        throw new OperationException(
                "Illegal state error for restore documents operation. The context stack doesn't contains a documents variable with the name "
                        + name);
    }

}
