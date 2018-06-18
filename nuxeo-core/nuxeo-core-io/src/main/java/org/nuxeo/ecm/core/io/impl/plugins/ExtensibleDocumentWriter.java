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

package org.nuxeo.ecm.core.io.impl.plugins;

import static org.nuxeo.ecm.core.api.CoreSession.IMPORT_VERSION_CREATED;
import static org.nuxeo.ecm.core.api.CoreSession.IMPORT_VERSION_DESCRIPTION;
import static org.nuxeo.ecm.core.api.CoreSession.IMPORT_VERSION_LABEL;
import static org.nuxeo.ecm.core.api.CoreSession.IMPORT_VERSION_VERSIONABLE_ID;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.io.ExportConstants;
import org.nuxeo.ecm.core.io.ExportExtension;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.ImportExtension;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;

/**
 * Compared to the default {@link DocumentModelWriter} implementation this one does handle versions and allows to plug
 * {@link ExportExtension}
 *
 * @since 7.4
 */
public class ExtensibleDocumentWriter extends DocumentModelWriter {

    protected static Log log = LogFactory.getLog(ExtensibleDocumentWriter.class);

    public ExtensibleDocumentWriter(CoreSession session, String parentPath) {
        super(session, parentPath);
    }

    protected List<ImportExtension> extensions = new ArrayList<ImportExtension>();

    public void registerExtension(ImportExtension ext) {
        extensions.add(ext);
    }

    @Override
    protected DocumentModel createDocument(ExportedDocument xdoc, Path toPath) {
        Path parentPath = toPath.removeLastSegments(1);
        String name = toPath.lastSegment();

        DocumentModel doc = session.createDocumentModel(parentPath.toString(), name, xdoc.getType());

        // set lifecycle state at creation
        Element system = xdoc.getDocument().getRootElement().element(ExportConstants.SYSTEM_TAG);
        String lifeCycleState = system.element(ExportConstants.LIFECYCLE_STATE_TAG).getText();
        String lifeCyclePolicy = system.element(ExportConstants.LIFECYCLE_POLICY_TAG).getText();

        doc.putContextData(CoreSession.IMPORT_LIFECYCLE_POLICY, lifeCyclePolicy);
        doc.putContextData(CoreSession.IMPORT_LIFECYCLE_STATE, lifeCycleState);

        // loadFacets before schemas so that additional schemas are not skipped
        loadFacetsInfo(doc, xdoc.getDocument());

        // then load schemas data
        loadSchemas(xdoc, doc, xdoc.getDocument());

        if (doc.hasSchema("uid")) {
            doc.putContextData(VersioningService.SKIP_VERSIONING, true);
        }

        String uuid = xdoc.getId();
        if (uuid != null) {
            ((DocumentModelImpl) doc).setId(uuid);
        }

        Element version = xdoc.getDocument().getRootElement().element("version");
        if (version != null) {

            Element e = version.element("isVersion");
            String isVersion = version.elementText("isVersion");

            if ("true".equals(isVersion)) {
                String label = version.elementText(IMPORT_VERSION_LABEL.substring(4));
                String sourceId = version.elementText(IMPORT_VERSION_VERSIONABLE_ID.substring(4));
                String desc = version.elementText(IMPORT_VERSION_DESCRIPTION.substring(4));
                String created = version.elementText(IMPORT_VERSION_CREATED.substring(4));

                if (label != null) {
                    doc.putContextData(IMPORT_VERSION_LABEL, label);
                }
                if (sourceId != null) {
                    doc.putContextData(IMPORT_VERSION_VERSIONABLE_ID, sourceId);
                }
                if (desc != null) {
                    doc.putContextData(IMPORT_VERSION_DESCRIPTION, desc);
                }
                if (created != null) {
                    doc.putContextData(IMPORT_VERSION_CREATED,
                            (Serializable) new DateType().decode(created));
                }
                doc.setPathInfo(null, name);
                ((DocumentModelImpl) doc).setIsVersion(true);

                doc.putContextData(CoreSession.IMPORT_VERSION_MAJOR,
                        doc.getPropertyValue("uid:major_version"));
                doc.putContextData(CoreSession.IMPORT_VERSION_MINOR,
                        doc.getPropertyValue("uid:minor_version"));
                doc.putContextData(CoreSession.IMPORT_IS_VERSION, true);
            }
        }

        if (doc.getId() != null) {
            session.importDocuments(Collections.singletonList(doc));
        } else {
            doc = session.createDocument(doc);
        }

        // load into the document the system properties, document needs to exist
        loadSystemInfo(doc, xdoc.getDocument());

        for (ImportExtension ext : extensions) {
            try {
                ext.updateImport(session, doc, xdoc);
            } catch (Exception e) {
                log.error("Error while processing extensions", e);
                throw new NuxeoException(e);
            }
        }

        unsavedDocuments += 1;
        saveIfNeeded();

        return doc;
    }

}
