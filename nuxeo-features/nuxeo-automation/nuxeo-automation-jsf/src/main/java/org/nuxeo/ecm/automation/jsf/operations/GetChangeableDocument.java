/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.automation.jsf.operations;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.jsf.OperationHelper;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @since 5.4.2
 */
@Operation(id = GetChangeableDocument.ID, category = Constants.CAT_FETCH, requires = Constants.SEAM_CONTEXT, label = "UI Changeable Document", description = "Get the current changeable document from the UI context. "
        + "The changeable document is used on creation forms.")
public class GetChangeableDocument {

    public static final String ID = "Seam.GetChangeableDocument";

    @OperationMethod
    public DocumentModel run() {
        return OperationHelper.getNavigationContext().getChangeableDocument();
    }

}
