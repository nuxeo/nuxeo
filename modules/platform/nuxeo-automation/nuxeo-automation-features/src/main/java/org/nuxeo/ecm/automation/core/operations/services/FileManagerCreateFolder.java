/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.automation.core.operations.services;

import java.io.IOException;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;

/**
 * Use {@link FileManager} to create a folder and set multiple properties on it.
 *
 * @since 11.1
 */
@Operation(id = FileManagerCreateFolder.ID, category = Constants.CAT_SERVICES, label = "Create Folder", description = "Create a Folder using the FileManagerService and set multiple properties on it."
        + "<p>The properties are specified as <i>key=value</i> pairs separated by a new line. "
        + "The key used for a property is the property xpath. "
        + "To specify multi-line values you can use a \\ character followed by a new line. "
        + "<p>Example:<pre>dc:title=The Folder Title<br>dc:description=foo bar</pre>"
        + "For updating a date, you will need to expose the value as ISO 8601 format, "
        + "for instance : <p>Example:<pre>dc:title=The Folder Title<br>dc:issued=@{org.nuxeo.ecm.core.schema.utils.DateParser.formatW3CDateTime(CurrentDate.date)}</pre>"
        + "<p>To update a multi-valued field with multiple values:<pre>custom:multivalued=a,b,c,d</pre>"
        + "<p>Returns back the created folder.")
public class FileManagerCreateFolder {

    public static final String ID = "FileManager.CreateFolder";

    @Context
    protected CoreSession session;

    @Context
    protected AutomationService as;

    @Context
    protected FileManager fileManager;

    @Context
    protected OperationContext context;

    @Param(name = "title")
    protected String title;

    @Param(name = "overwrite", required = false, description = "Whether to overwrite an existing folder with the same title, defaults to false")
    protected boolean overwrite = false;

    @Param(name = "properties", required = false)
    protected Properties properties;

    @OperationMethod
    public DocumentModel run(DocumentModel parent) throws IOException {
        DocumentModel doc = fileManager.createFolder(session, title, parent.getPathAsString(), overwrite);

        if (properties != null) {
            DocumentHelper.setProperties(session, doc, properties);
            if (doc.isDirty()) {
                doc = session.saveDocument(doc);
            }
        }

        return doc;
    }
}
