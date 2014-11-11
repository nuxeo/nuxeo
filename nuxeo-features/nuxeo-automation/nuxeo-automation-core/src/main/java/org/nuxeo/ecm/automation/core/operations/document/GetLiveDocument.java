/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:bjalon@nuxeo.com">Benjamin JALON</a>
 * @since 5.7
 */
@Operation(id = GetLiveDocument.ID, category = Constants.CAT_DOCUMENT, label = "Get Live Document", description = "Get the live document even if this is a Proxy or Version Document.")
public class GetLiveDocument {

    public static final String ID = "GetLiveDocument";

    private static int MAX_ITERATION = 5;

    @Context
    protected CoreSession session;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel input) throws ClientException {
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
