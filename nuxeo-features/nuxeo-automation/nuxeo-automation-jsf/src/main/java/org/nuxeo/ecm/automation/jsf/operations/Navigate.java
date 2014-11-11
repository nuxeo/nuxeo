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
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@Operation(id = Navigate.ID, category = Constants.CAT_UI, requires = Constants.SEAM_CONTEXT, label = "Navigate to Document", description = "Navigate to the input document. The outcome of the UI action will be stored in the operation chain context as the 'Outcome' variable. Returns back the document.")
public class Navigate {

    public static final String ID = "Seam.NavigateTo";

    @Context
    protected OperationContext ctx;

    @Param(name = "view", required = false)
    protected String view;

    @OperationMethod
    public DocumentModel run(DocumentModel doc) throws Exception {
        String outcome = null;

        if (view == null) {
            outcome = OperationHelper.getNavigationContext().navigateToDocument(
                    doc);
        } else {
            outcome = OperationHelper.getNavigationContext().navigateToDocument(
                    doc, view);
        }

        ctx.put(SeamOperation.OUTCOME, outcome);

        return doc;
    }
}
