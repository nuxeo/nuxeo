/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.versioning.VersioningService;

/**
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = CreateVersion.ID, category = Constants.CAT_DOCUMENT, label = "Snapshot Version", description = "Create a new version for the input document. Any modification made on the document by the chain will be automatically saved. Increment version if this was specified through the 'snapshot' parameter. Returns the live document (not the version).")
public class CreateVersion {

    public static final String ID = "Document.CreateVersion";

    @Context
    protected CoreSession session;

    @Param(name = "increment", required = false, widget = Constants.W_OPTION, values = {
            "None", "Minor", "Major" })
    protected String snapshot = "None";

    @Param(name = "saveDocument", required = false, widget = Constants.W_CHECK, description = "Save the document in the session after versioning")
    protected boolean saveDocument = true;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) throws Exception {
        if (!doc.hasFacet(FacetNames.VERSIONABLE)) {
            throw new ClientException(
                    String.format(
                            "The document (id:'%s') with title '%s' doesn't have 'versionable' facet",
                            doc.getId(), doc.getTitle()));
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
