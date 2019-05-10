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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.automation.core.operations.document;

import java.util.Locale;
import java.util.Map;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersioningOption;

/**
 * Check in the input document.
 */
@Operation(id = CheckInDocument.ID, category = Constants.CAT_DOCUMENT, label = "Check In", description = "Checks in the input document. Returns back the document.")
public class CheckInDocument {

    public static final String ID = "Document.CheckIn";

    public static final Map<String, VersioningOption> options = Map.of("none", VersioningOption.NONE, "minor",
            VersioningOption.MINOR, "major", VersioningOption.MAJOR);

    @Context
    protected OperationContext ctx;

    @Context
    protected CoreSession session;

    @Param(name = "version", required = true, values = { "none", "minor", "major" }, order = 0)
    protected String version;

    @Param(name = "comment", required = false, order = 1)
    protected String comment;

    @Param(name = "versionVarName", required = false, order = 2)
    protected String versionVarName;

    protected VersioningOption getVersioningOption() {
        return options.getOrDefault(version.toLowerCase(Locale.ENGLISH), VersioningOption.MINOR);
    }

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentRef doc) {
        if (session.isCheckedOut(doc)) {
            DocumentRef ver = session.checkIn(doc, getVersioningOption(), comment);
            if (versionVarName != null) {
                ctx.put(versionVarName, ver);
            }
        } else {
            if (versionVarName != null) {
                ctx.put(versionVarName, session.getLastDocumentVersion(doc));
            }
        }
        return session.getDocument(doc);
    }

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) {
        if (doc.isCheckedOut()) {
            DocumentRef ver = session.checkIn(doc.getRef(), getVersioningOption(), comment);
            doc.refresh(DocumentModel.REFRESH_STATE, null);
            if (versionVarName != null) {
                ctx.put(versionVarName, ver);
            }
        } else {
            if (versionVarName != null) {
                ctx.put(versionVarName, session.getLastDocumentVersion(doc.getRef()));
            }
        }
        return doc;
    }

}
