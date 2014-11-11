/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mariana Cedica
 */
package org.nuxeo.ecm.automation.core.test;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.operations.services.PaginableDocumentModelListImpl;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;

/**
 * @since 5.6
 */
@Operation(id = "pushElementList")
public class PushListElementsOperation {

    @Context
    OperationContext ctx;

    @OperationMethod
    public void run(PaginableDocumentModelListImpl list) {
        DocumentModelListImpl result = (DocumentModelListImpl) ctx.get("result");
        if (result == null) {
            result = new DocumentModelListImpl();
        }
        for (DocumentModel documentModel : list) {
            result.add(documentModel);
        }
        ctx.put("result", result);
    }
}
