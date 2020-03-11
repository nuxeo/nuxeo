/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.template.service;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

public class TemplateMappingRemover extends UnrestrictedSessionRunner {

    protected DocumentRef targetRef;

    protected String type2Remove;

    protected TemplateMappingRemover(CoreSession session, DocumentModel doc, String type2Remove) {
        super(session);
        targetRef = doc.getRef();
        this.type2Remove = type2Remove;
    }

    protected TemplateMappingRemover(CoreSession session, String uid, String type2Remove) {
        super(session);
        targetRef = new IdRef(uid);
        this.type2Remove = type2Remove;
    }

    @Override
    public void run() {
        DocumentModel doc = session.getDocument(targetRef);
        TemplateSourceDocument source = doc.getAdapter(TemplateSourceDocument.class);
        source.removeForcedType(type2Remove, true);
    }

}
