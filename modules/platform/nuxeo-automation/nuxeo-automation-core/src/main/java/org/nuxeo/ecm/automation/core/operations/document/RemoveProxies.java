/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Frederic Vadon
 *     Ricardo Dias
 */
package org.nuxeo.ecm.automation.core.operations.document;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * @since 8.3
 */
@Operation(id = RemoveProxies.ID, category = Constants.CAT_DOCUMENT, label = "Remove Document Proxies", description = "Will remove all proxies pointing on the input document. Useful for instance to unpublish a document. Notice: this operation will remove all proxies, including the ones pointing to the current document version (live proxies). Activating the save parameter forces the changes to be written in database immediately (at the cost of performance loss).", aliases = {"Document.RemoveProxies"})
public class RemoveProxies {

    public static final String ID = "Document.RemoveProxies";

    @Context
    protected CoreSession session;

    @Param(name = "save", required = false, values = { "true" })
    protected boolean save = true;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel input) {
        DocumentModelList proxies = session.getProxies(input.getRef(), null);
        for (DocumentModel proxy : proxies) {
            session.removeDocument(proxy.getRef());
        }

        if (save) {
            session.save();
        }
        return input;
    }

}
