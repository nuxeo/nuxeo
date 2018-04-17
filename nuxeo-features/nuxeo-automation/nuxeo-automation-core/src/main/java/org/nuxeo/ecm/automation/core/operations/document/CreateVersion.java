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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.operations.document;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.versioning.VersioningService;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = CreateVersion.ID, category = Constants.CAT_DOCUMENT, label = "Snapshot Version", description = "Create a new version for the input document. Any modification made on the document by the chain will be automatically saved. Increment version if this was specified through the 'snapshot' parameter. This operation should not be used in the context of the empty document created, about to create, before document modification, document modified events. Returns the live document (not the version).")
public class CreateVersion {

    public static final String ID = "Document.CreateVersion";

    @Context
    protected CoreSession session;

    @Param(name = "increment", required = false, widget = Constants.W_OPTION, values = { "None", "Minor", "Major" })
    protected String snapshot = "None";

    @Param(name = "saveDocument", required = false, widget = Constants.W_CHECK, description = "Save the document in the session after versioning")
    protected boolean saveDocument = true;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) {
        if (!doc.hasFacet(FacetNames.VERSIONABLE)) {
            throw new NuxeoException(String.format(
                    "The document (id:'%s') with title '%s' doesn't have 'versionable' facet", doc.getId(),
                    doc.getTitle()));
        }
        VersioningOption vo;
        if ("Minor".equalsIgnoreCase(snapshot)) {
            vo = VersioningOption.MINOR;
        } else if ("Major".equalsIgnoreCase(snapshot)) {
            vo = VersioningOption.MAJOR;
        } else {
            vo = null;
        }
        if (vo != null) {
            doc.putContextData(VersioningService.VERSIONING_OPTION, vo);
        }

        if (saveDocument) {
            return DocumentHelper.saveDocument(session, doc);
        } else {
            return doc;
        }
    }

}
