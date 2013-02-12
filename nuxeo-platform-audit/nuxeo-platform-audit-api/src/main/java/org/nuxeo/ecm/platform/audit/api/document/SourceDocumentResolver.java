/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.platform.audit.api.document;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;

/**
 * Simple helper to fetch the target Audited document
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class SourceDocumentResolver extends UnrestrictedSessionRunner {

    public DocumentModel sourceDocument;

    protected final DocumentModel document;

    SourceDocumentResolver(CoreSession session, DocumentModel doc) {
        super(session);
        this.document = doc;
    }

    @Override
    public void run() throws ClientException {
        DocumentModel version = null;
        if (document.isProxy()) {
            version = session.getSourceDocument(document.getRef());
        } else {
            version = document;
        }
        if (version != null) {
            if (version.getSourceId() != null
                    && session.exists(new IdRef(version.getSourceId()))) {
                sourceDocument = session.getSourceDocument(version.getRef());
            }
        }
    }

}
