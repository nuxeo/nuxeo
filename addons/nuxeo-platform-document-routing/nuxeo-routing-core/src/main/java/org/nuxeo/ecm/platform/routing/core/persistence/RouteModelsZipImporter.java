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

import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_TYPE;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipFile;

import org.apache.commons.codec.digest.DigestUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
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
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;

/**
 * Imports a route document from a zip archive using the IO core service . Existing route model with the same path as
 * the are one to be imported is deleted before import.
 *
 * @since 5.6
 */
public class RouteModelsZipImporter extends ExportedZipImporter {

    private static final long serialVersionUID = 1L;

    /** @since 11.5 */
    public static final String WORKFLOW_KEY_VALUE_STORE = "workflowModels";

    @Override
    public DocumentModel createOrUpdate(FileImporterContext context) throws IOException {
        try (CloseableFile source = context.getBlob().getCloseableFile()) {
            ZipFile zip = getArchiveFileIfValid(source.getFile());
            if (zip == null) {
                return null;
            }
            zip.close();

            KeyValueStore workflowKV = Framework.getService(KeyValueService.class)
                                                .getKeyValueStore(WORKFLOW_KEY_VALUE_STORE);
            String parentPath = context.getParentPath();
            CoreSession session = context.getSession();

            ExportedDocument rootDoc = getRootExportedDocument(source.getFile());
            if (!DOCUMENT_ROUTE_DOCUMENT_TYPE.equals(rootDoc.getType())) {
                return null;
            }

            Path rootPath = rootDoc.getPath();
            String rootName = rootPath.lastSegment();
            PathRef rootRef = new PathRef(parentPath, rootName);
            String rootDigest = getMD5Digest(source.getFile());
            String kvDigestKey = "digest-" + rootName;

            ACP currentRouteModelACP = null;
            if (session.exists(rootRef)) {
                DocumentModel target = session.getDocument(rootRef);
                // check that the workflow to import has changed before continuing
                if (rootDigest.equals(workflowKV.getString(kvDigestKey))) {
                    return target;
                }
                // workflow has changed, backup the ACP and clean up the route before import
                currentRouteModelACP = target.getACP();
                session.removeDocument(rootRef);
            }

            DocumentWriter writer = new DocumentModelWriter(session, parentPath, 10);
            DocumentReader reader = new NuxeoArchiveReader(source.getFile());

            try {
                DocumentPipe pipe = new DocumentPipeImpl(10);
                pipe.setReader(reader);
                pipe.setWriter(writer);
                pipe.run();
            } finally {
                reader.close();
                writer.close();
            }

            workflowKV.put(kvDigestKey, rootDigest);

            DocumentModel newRouteModel = session.getDocument(rootRef);
            if (currentRouteModelACP != null && context.isOverwrite()) {
                newRouteModel.setACP(currentRouteModelACP, true);
                newRouteModel = session.saveDocument(newRouteModel);
            }
            return newRouteModel;
        }
    }

    protected ExportedDocument getRootExportedDocument(File file) throws IOException {
        DocumentReader reader = new NuxeoArchiveReader(file);
        ExportedDocument root = reader.read();
        reader.close();
        return root;
    }

    protected String getMD5Digest(File file) {
        try (InputStream in = new FileInputStream(file)) {
            return DigestUtils.md5Hex(in);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

}
