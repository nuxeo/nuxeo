/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Mariana Cedica
 */
package org.nuxeo.ecm.platform.routing.core.persistence;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelWriter;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveReader;
import org.nuxeo.ecm.platform.filemanager.service.extension.ExportedZipImporter;
import org.nuxeo.ecm.platform.types.TypeManager;

/**
 *
 * Imports a route document from a zip archive using the IO core service .
 * Existing route model with the same path as the are one to be imported is
 * deleted before import.
 *
 * @since 5.6
 *
 */
public class RouteModelsZipImporter extends ExportedZipImporter {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(RouteModelsZipImporter.class);

    @Override
    public DocumentModel create(CoreSession session, Blob content, String path,
            boolean overwrite, String filename, TypeManager typeService)
            throws ClientException, IOException {

        File tmp = File.createTempFile("xml-importer", null);
        content.transferTo(tmp);
        ZipFile zip = getArchiveFileIfValid(tmp);

        if (zip == null) {
            tmp.delete();
            return null;
        }

        boolean overWrite = false;
        DocumentReader reader = new NuxeoArchiveReader(tmp);
        ExportedDocument root = reader.read();
        PathRef rootRef = new PathRef(path, root.getPath().toString());

        if (session.exists(rootRef)) {
            DocumentModel target = session.getDocument(rootRef);
            if (target.getPath().removeLastSegments(1).equals(new Path(path))) {
                overWrite = true;
                // clean up existing route before import
                session.removeDocument(rootRef);
            }
        }

        DocumentWriter writer = new DocumentModelWriter(session, path, 10);
        reader.close();
        reader = new NuxeoArchiveReader(tmp);

        DocumentRef resultingRef;
        if (overwrite && overWrite) {
            resultingRef = rootRef;
        } else {
            String rootName = root.getPath().lastSegment();
            resultingRef = new PathRef(path + "/" + rootName);
        }

        try {
            DocumentPipe pipe = new DocumentPipeImpl(10);
            pipe.setReader(reader);
            pipe.setWriter(writer);
            pipe.run();
        } catch (Exception e) {
             log.error(e, e);
        } finally {
            reader.close();
            writer.close();
        }
        tmp.delete();
        return session.getDocument(resultingRef);
    }

}
