/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benjamin JALON <bjalon@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.document;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:bjalon@nuxeo.com">Benjamin JALON</a>
 * @since 5.7
 */
@Operation(id = GetLiveDocument.ID, category = Constants.CAT_DOCUMENT, label = "Get Live Document", description = "Get the live document even if this is a Proxy or Version Document.", aliases = { "GetLiveDocument" })
public class GetLiveDocument {

    public static final String ID = "Proxy.GetSourceDocument";

    private static int MAX_ITERATION = 5;

    @Context
    protected CoreSession session;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel input) {
        DocumentModel doc = session.getSourceDocument(input.getRef());
        for (int i = 0; i < MAX_ITERATION && !isLive(doc); i++) {
            doc = session.getSourceDocument(doc.getRef());
        }

        return doc;
    }

    private boolean isLive(DocumentModel doc) {
        return !doc.isVersion() && !doc.isProxy();
    }

}
