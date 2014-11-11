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

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.jsf.OperationHelper;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@Operation(id = GetCurrentDocument.ID, category = Constants.CAT_FETCH, requires = Constants.SEAM_CONTEXT, label = "UI Current Document", description = "Get the current Document from the UI context.")
public class GetCurrentDocument {

    public static final String ID = "Seam.GetCurrentDocument";

    @OperationMethod
    public DocumentModel run() {
        return OperationHelper.getNavigationContext().getCurrentDocument();
    }

}
