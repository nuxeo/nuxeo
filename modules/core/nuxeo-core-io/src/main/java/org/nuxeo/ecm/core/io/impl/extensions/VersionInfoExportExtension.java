/*
 * Copyright (c) 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.io.impl.extensions;

import static org.nuxeo.ecm.core.api.CoreSession.IMPORT_VERSION_CREATED;
import static org.nuxeo.ecm.core.api.CoreSession.IMPORT_VERSION_DESCRIPTION;
import static org.nuxeo.ecm.core.api.CoreSession.IMPORT_VERSION_LABEL;
import static org.nuxeo.ecm.core.api.CoreSession.IMPORT_VERSION_VERSIONABLE_ID;

import java.util.List;

import org.dom4j.Element;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.io.ExportExtension;
import org.nuxeo.ecm.core.io.impl.ExportedDocumentImpl;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;

/**
 * Exports version information for a given {@link DocumentModel}
 *
 * @since 7.4
 */
public class VersionInfoExportExtension implements ExportExtension {

    @Override
    public void updateExport(DocumentModel docModel, ExportedDocumentImpl result) throws Exception {

        Element versionElement = result.getDocument().getRootElement().addElement("version");

        if (docModel.isVersion()) {
            // IMPORT_VERSION_LABEL
            versionElement.addElement("isVersion").setText("true");
            versionElement.addElement(IMPORT_VERSION_LABEL.substring(4)).setText(docModel.getVersionLabel());

            // IMPORT_VERSION_VERSIONABLE_ID
            String sourceId = docModel.getSourceId();
            versionElement.addElement(IMPORT_VERSION_VERSIONABLE_ID.substring(4)).setText(sourceId);
            DocumentModel liveDocument = docModel.getCoreSession().getSourceDocument(docModel.getRef());

            List<VersionModel> versions = docModel.getCoreSession().getVersionsForDocument(liveDocument.getRef());
            for (VersionModel version : versions) {
                if (!docModel.getVersionLabel().equals(version.getLabel())) {
                    continue;
                }
                // IMPORT_VERSION_DESCRIPTION
                String description = version.getDescription();
                if (description != null) {
                    versionElement.addElement(IMPORT_VERSION_DESCRIPTION.substring(4)).setText(description);
                }

                // IMPORT_VERSION_CREATED
                if (version.getCreated() != null) {
                    String created = new DateType().encode(version.getCreated());
                    versionElement.addElement(IMPORT_VERSION_CREATED.substring(4)).setText(created);
                }
                break;
            }
        }
    }
}
