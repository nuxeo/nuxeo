/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.platform.audit.api.document;

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
    public void run() {
        DocumentModel version = null;
        if (document.isProxy()) {
            version = session.getSourceDocument(document.getRef());
        } else {
            version = document;
        }
        if (version != null) {
            if (version.getSourceId() != null && session.exists(new IdRef(version.getSourceId()))) {
                sourceDocument = session.getSourceDocument(version.getRef());
            }
        }
    }

}
