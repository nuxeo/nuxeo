/*
 * (C) Copyright 2006-2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */package org.nuxeo.ecm.platform.importer.xml.parser;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.importer.base.GenericMultiThreadedImporter;
import org.nuxeo.ecm.platform.importer.factories.ImporterDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.source.FileSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.ecm.platform.scanimporter.processor.ScanedFileFactory;
import org.nuxeo.ecm.platform.scanimporter.processor.ScannedFileImporter;
import org.nuxeo.ecm.platform.scanimporter.service.ImporterConfig;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * Replace default XML Parser used into the Scan Importer service by the
 * advanced one implemented into nuxeo-importer-xml-parser
 *
 * @author Benjamin JALON
 *
 */
public class AdvancedScannedFileFactory extends ScanedFileFactory implements
        ImporterDocumentModelFactory {

    public AdvancedScannedFileFactory(ImporterConfig config) {
        super(config);
    }

    protected static String targetContainerType = null;

    protected ImporterConfig config;

    @Override
    public DocumentModel createLeafNode(CoreSession session,
            DocumentModel parent, SourceNode node) throws Exception {

        XMLImporterService importer = Framework.getLocalService(XMLImporterService.class);
        if (!(node instanceof FileSourceNode)) {
            throw new ClientException("Waiting a FileSourceNode object not: "
                    + node.getClass().getName());
        }
        FileSourceNode fileNode = (FileSourceNode) node;
        List<DocumentModel> docCreated = importer.importDocuments(parent,
                fileNode.getFile());

        if (docCreated == null || docCreated.size() < 1) {
            return null;
        }

        ScannedFileImporter.addProcessedDescriptor(fileNode.getSourcePath());

        GenericMultiThreadedImporter.addCreatedDoc("_XMLimporter",
                docCreated.size() - 1);
        return docCreated.get(0);
    }
}
