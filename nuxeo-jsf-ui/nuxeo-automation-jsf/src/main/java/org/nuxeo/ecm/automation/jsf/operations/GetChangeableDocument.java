/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
