/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */
package org.nuxeo.ecm.automation.core.operations.services;

import java.io.IOException;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.filemanager.api.FileImporterContext;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;

/**
 * Use {@link FileManager} to create documents from blobs and set multiple properties on them.
 *
 * @since 10.10
 */
@Operation(id = FileManagerImportWithProperties.ID, category = Constants.CAT_SERVICES, label = "Create Document from file", description = "Create Document(s) from Blob(s) using the FileManagerService and set multiple properties on them."
        + "The destination container must be passed in a Context variable named currentDocument. "
        + "<p>The properties are specified as <i>key=value</i> pairs separated by a new line. "
        + "The key used for a property is the property xpath. "
        + "To specify multi-line values you can use a \\ character followed by a new line. "
        + "<p>Example:<pre>dc:title=The Document Title<br>dc:description=foo bar</pre>"
        + "For updating a date, you will need to expose the value as ISO 8601 format, "
        + "for instance : <p>Example:<pre>dc:title=The Document Title<br>dc:issued=@{org.nuxeo.ecm.core.schema.utils.DateParser.formatW3CDateTime(CurrentDate.date)}</pre><p>Returns back the updated document."
        + "<p>To update a multi-valued field with multiple values:<pre>custom:multivalued=a,b,c,d</pre>")
public class FileManagerImportWithProperties {

    public static final String ID = "FileManager.ImportWithProperties";

    @Context
    protected CoreSession session;

    @Context
    protected FileManager fileManager;

    @Context
    protected AutomationService as;

    @Context
    protected OperationContext context;

    @Param(name = "overwrite", required = false, description = "Whether to overwrite an existing file with the same title, defaults to false")
    protected boolean overwrite = false;

    @Param(name = "mimeTypeCheck", required = false, description = "Whether to check the blob's mime-type against the file name, defaults to true")
    protected boolean mimeTypeCheck = true;

    @Param(name = "properties")
    protected Properties properties;

    protected DocumentModel getCurrentDocument() throws OperationException {
        String cdRef = (String) context.get("currentDocument");
        return as.getAdaptedValue(context, cdRef, DocumentModel.class);
    }

    @OperationMethod
    public DocumentModel run(Blob blob) throws OperationException, IOException {
        DocumentModel currentDocument = getCurrentDocument();
        String path = currentDocument.getPathAsString();
        FileImporterContext fileCreationContext = FileImporterContext.builder(session, blob, path)
                                                                     .overwrite(overwrite)
                                                                     .mimeTypeCheck(mimeTypeCheck)
                                                                     .persistDocument(false)
                                                                     .build();
        DocumentModel doc = fileManager.createOrUpdateDocument(fileCreationContext);
        DocumentHelper.setProperties(session, doc, properties);

        if (doc.isDirty()) {
            doc = doc.getId() == null ? session.createDocument(doc) : session.saveDocument(doc);
        }

        return doc;
    }

    @OperationMethod
    public DocumentModelList run(BlobList blobs) throws OperationException, IOException {
        DocumentModelList result = new DocumentModelListImpl();
        for (Blob blob : blobs) {
            result.add(run(blob));
        }
        return result;
    }

}
