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
import org.nuxeo.ecm.automation.jsf.OperationHelper;

@Operation(id = CreateDocumentForm.ID, category = Constants.CAT_UI, requires = Constants.SEAM_CONTEXT, label = "Show Create Document Page", description = "Show the document creation form given a type. This is a void operation - the input object is returned back as the oputput")
public class CreateDocumentForm {

    public static final String ID = "Seam.CreateDocumentForm";

    @Context
    protected OperationContext ctx;

    @Param(name = "type")
    protected String type;

    @OperationMethod
    public void run() throws Exception {
        ctx.put(SeamOperation.OUTCOME,
                OperationHelper.getDocumentActions().createDocument(type));
    }

}
