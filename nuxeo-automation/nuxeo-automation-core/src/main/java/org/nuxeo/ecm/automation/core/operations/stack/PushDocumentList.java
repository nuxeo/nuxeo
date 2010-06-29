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
@Operation(id = PushDocumentList.ID, category = Constants.CAT_EXECUTION_STACK, label = "Push Document List", description = "Push the input document list on the context stack. The document list can be restored later as the input using the corrresponding pop operation. Returns the input document list.")
public class PushDocumentList {

    public static final String ID = "Document.PushList";

    @Context
    protected OperationContext ctx;

    @OperationMethod
    public DocumentModelList run(DocumentModelList doc) throws Exception {
        ctx.push(Constants.O_DOCUMENTS, doc);
        return doc;
    }

    @OperationMethod
    public DocumentRefList run(DocumentRefList doc) throws Exception {
        ctx.push(Constants.O_DOCUMENTS, doc);
        return doc;
    }

}
