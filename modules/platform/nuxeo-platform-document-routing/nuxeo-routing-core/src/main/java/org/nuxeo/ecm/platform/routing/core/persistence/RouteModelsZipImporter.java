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
 *     Mariana Cedica
 */
package org.nuxeo.ecm.platform.routing.core.persistence;

import java.io.IOException;
import java.util.zip.ZipFile;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelWriter;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveReader;
import org.nuxeo.ecm.platform.filemanager.api.FileImporterContext;
import org.nuxeo.ecm.platform.filemanager.service.extension.ExportedZipImporter;

/**
 * Imports a route document from a zip archive using the IO core service . Existing route model with the same path as
 * the are one to be imported is deleted before import.
 *
 * @since 5.6
 */
public class RouteModelsZipImporter extends ExportedZipImporter {

    private static final long serialVersionUID = 1L;

    @Override
    public DocumentModel createOrUpdate(FileImporterContext context) throws IOException {
        try (CloseableFile source = context.getBlob().getCloseableFile()) {
            ZipFile zip = getArchiveFileIfValid(source.getFile());
            if (zip == null) {
                return null;
            }
            zip.close();

            boolean overWrite = false;
            DocumentReader reader = new NuxeoArchiveReader(source.getFile());
            ExportedDocument root = reader.read();
            String parentPath = context.getParentPath();
            PathRef rootRef = new PathRef(parentPath, root.getPath().toString());
            ACP currentRouteModelACP = null;
            CoreSession session = context.getSession();
            if (session.exists(rootRef)) {
                DocumentModel target = session.getDocument(rootRef);
                if (target.getPath().removeLastSegments(1).equals(new Path(parentPath))) {
                    overWrite = true;
                    // clean up existing route before import
                    DocumentModel routeModel = session.getDocument(rootRef);
                    currentRouteModelACP = routeModel.getACP();
                    session.removeDocument(rootRef);
                }
            }

            DocumentWriter writer = new DocumentModelWriter(session, parentPath, 10);
            reader.close();
            reader = new NuxeoArchiveReader(source.getFile());

            DocumentRef resultingRef;
            if (context.isOverwrite() && overWrite) {
                resultingRef = rootRef;
            } else {
                String rootName = root.getPath().lastSegment();
                resultingRef = new PathRef(parentPath, rootName);
            }

            try {
                DocumentPipe pipe = new DocumentPipeImpl(10);
                pipe.setReader(reader);
                pipe.setWriter(writer);
                pipe.run();
            } finally {
                reader.close();
                writer.close();
            }

            DocumentModel newRouteModel = session.getDocument(resultingRef);
            if (currentRouteModelACP != null && context.isOverwrite() && overWrite) {
                newRouteModel.setACP(currentRouteModelACP, true);
            }
            return session.saveDocument(newRouteModel);
        }
    }

}
