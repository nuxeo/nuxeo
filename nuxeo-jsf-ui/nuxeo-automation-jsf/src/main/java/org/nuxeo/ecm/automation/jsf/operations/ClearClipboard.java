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
 */

package org.nuxeo.ecm.automation.jsf.operations;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.jsf.OperationHelper;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;

/**
 * @author Anahide Tchertchian
 */
@Operation(id = ClearClipboard.ID, category = Constants.CAT_UI, requires = Constants.SEAM_CONTEXT, label = "Clear Clipboard", description = "Clear the clipboard content.", aliases = {
        "WebUI.ClearClipboard" })
public class ClearClipboard {

    public static final String ID = "Seam.ClearClipboard";

    @OperationMethod
    public void run() {
        OperationHelper.getDocumentListManager().resetWorkingList(DocumentsListsManager.CLIPBOARD);
    }

}
