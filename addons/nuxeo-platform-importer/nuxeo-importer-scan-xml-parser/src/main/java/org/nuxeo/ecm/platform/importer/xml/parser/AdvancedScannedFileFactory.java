/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */package org.nuxeo.ecm.platform.importer.xml.parser;

import java.io.IOException;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.importer.base.GenericMultiThreadedImporter;
import org.nuxeo.ecm.platform.importer.factories.ImporterDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.source.FileSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.ecm.platform.scanimporter.processor.ScanedFileFactory;
import org.nuxeo.ecm.platform.scanimporter.processor.ScannedFileImporter;
import org.nuxeo.ecm.platform.scanimporter.service.ImporterConfig;
import org.nuxeo.runtime.api.Framework;

/**
 * Replace default XML Parser used into the Scan Importer service by the advanced one implemented into
 * nuxeo-importer-xml-parser
 *
 * @author Benjamin JALON
 */
public class AdvancedScannedFileFactory extends ScanedFileFactory implements ImporterDocumentModelFactory {

    public AdvancedScannedFileFactory(ImporterConfig config) {
        super(config);
    }

    protected static String targetContainerType = null;

    @Override
    public DocumentModel createLeafNode(CoreSession session, DocumentModel parent, SourceNode node) throws IOException {

        XMLImporterService importer = Framework.getService(XMLImporterService.class);
        if (!(node instanceof FileSourceNode)) {
            throw new NuxeoException("Waiting a FileSourceNode object not: " + node.getClass().getName());
        }
        FileSourceNode fileNode = (FileSourceNode) node;
        List<DocumentModel> docCreated = importer.importDocuments(parent, fileNode.getFile());

        if (docCreated == null || docCreated.size() < 1) {
            return null;
        }

        ScannedFileImporter.addProcessedDescriptor(fileNode.getSourcePath());

        GenericMultiThreadedImporter.addCreatedDoc("_XMLimporter", docCreated.size() - 1);
        return docCreated.get(0);
    }
}
