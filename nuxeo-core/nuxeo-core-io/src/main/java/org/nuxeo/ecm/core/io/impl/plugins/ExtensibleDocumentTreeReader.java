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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentTreeIterator;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.ExportExtension;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.ExportedDocumentImpl;

/**
 * Compared to the default {@link DocumentReader} implementation this one does handle versions and allows to plug
 * {@link ExportExtension}
 *
 * @since 7.4
 */
public class ExtensibleDocumentTreeReader extends DocumentModelReader {

    protected DocumentTreeIterator iterator;

    protected int pathSegmentsToRemove = 0;

    protected List<DocumentModel> pendingVersions = new LinkedList<>();

    protected List<ExportExtension> extensions = new ArrayList<>();

    public static final String VERSION_VIRTUAL_PATH_SEGMENT = "__versions__";

    public ExtensibleDocumentTreeReader(CoreSession session, DocumentModel root, boolean excludeRoot) {
        super(session);
        iterator = new DocumentTreeIterator(session, root, excludeRoot);
        pathSegmentsToRemove = root.getPath().segmentCount() - (excludeRoot ? 0 : 1);
    }

    public ExtensibleDocumentTreeReader(CoreSession session, DocumentRef root) {
        this(session, session.getDocument(root));
    }

    public ExtensibleDocumentTreeReader(CoreSession session, DocumentModel root) {
        this(session, root, false);
    }

    public void registerExtension(ExportExtension ext) {
        extensions.add(ext);
    }

    @Override
    public void close() {
        super.close();
        iterator.reset();
        iterator = null;
    }

    @Override
    public ExportedDocument read() throws IOException {

        DocumentModel docModel = null;
        if (pendingVersions.size() > 0) {
            docModel = pendingVersions.remove(0);
        } else {
            if (iterator.hasNext()) {
                docModel = iterator.next();
                try {
                    List<DocumentModel> versions = session.getVersions(docModel.getRef());
                    if (!versions.isEmpty()) {
                        pendingVersions.addAll(0, versions);
                    }
                } catch (Exception e) {
                    throw new IOException("Unable to get versions", e);
                }
            }
        }

        ExportedDocumentImpl result = null;
        if (docModel != null) {
            if (pathSegmentsToRemove > 0) {
                // remove unwanted leading segments
                result = new ExportedDocumentImpl(docModel,
                        docModel.getPath().removeFirstSegments(pathSegmentsToRemove), inlineBlobs);
            } else {
                result = new ExportedDocumentImpl(docModel, inlineBlobs);
            }

            // flag versions
            if (docModel.isVersion()) {
                Path path = docModel.getPath().append(VERSION_VIRTUAL_PATH_SEGMENT).append(docModel.getVersionLabel());
                if (pathSegmentsToRemove > 0) {
                    path = path.removeFirstSegments(pathSegmentsToRemove);
                }
                result.setPath(path);
            }

            try {
                for (ExportExtension ext : extensions) {
                    ext.updateExport(docModel, result);
                }
            } catch (Exception e) {
                throw new IOException("Unable to process versions", e);
            }
        }
        return result;
    }

}
