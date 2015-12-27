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
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRefList;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = PushDocumentList.ID, category = Constants.CAT_EXECUTION_STACK, label = "Push Document List", description = "Push the input document list on the context stack. The document list can be restored later as the input using the corrresponding pop operation. Returns the input document list.", aliases = { "Document.PushList" })
public class PushDocumentList {

    public static final String ID = "Context.PushDocumentList";

    @Context
    protected OperationContext ctx;

    @OperationMethod
    public DocumentModelList run(DocumentModelList doc) {
        ctx.push(Constants.O_DOCUMENTS, doc);
        return doc;
    }

    @OperationMethod
    public DocumentRefList run(DocumentRefList doc) {
        ctx.push(Constants.O_DOCUMENTS, doc);
        return doc;
    }

}
