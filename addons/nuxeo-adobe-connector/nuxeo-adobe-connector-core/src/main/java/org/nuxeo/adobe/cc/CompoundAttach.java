/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Mariana Cedica
 */
package org.nuxeo.adobe.cc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @since 9.10
 */
@Operation(id = CompoundAttach.ID, category = Constants.CAT_DOCUMENT, label = "Attach compound docs", description = "Attach compund documents.")
public class CompoundAttach {

    public static final String ID = "CompoundDocument.Attach";

    private static final Log log = LogFactory.getLog(CompoundAttach.class);

    public static final String COMPOUND_DOC_FACET = "CompoundDocument";

    public static final String COMPOUND_DOC_DOCS_PROPERTY = "compound:docs";

    @Context
    protected CoreSession session;

    @Param(name = "compoundDocs")
    protected List<String> compoundDocs = new ArrayList<>();

    @Param(name = "save", required = false)
    protected Boolean save = true;

    @OperationMethod
    public DocumentModel run(DocumentModel doc) {
        if (!doc.hasFacet(COMPOUND_DOC_FACET)) {
            doc.addFacet(COMPOUND_DOC_FACET);
        }
        doc.setPropertyValue(COMPOUND_DOC_DOCS_PROPERTY, (Serializable) compoundDocs);
        if (save) {
            doc = session.saveDocument(doc);
        }
        return doc;
    }
}
