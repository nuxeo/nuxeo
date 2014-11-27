/*
 * (C) Copyright 2002-2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.importer.xml.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Main Nuxeo Runtime component managing extension points and exposing
 * {@link XMLImporterService}
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class XMLImporterComponent extends DefaultComponent implements
        XMLImporterService {

    protected List<DocConfigDescriptor> docConfigs = new ArrayList<DocConfigDescriptor>();

    protected List<AttributeConfigDescriptor> attributeConfigs = new ArrayList<AttributeConfigDescriptor>();

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if ("documentMapping".equals(extensionPoint)) {
            docConfigs.add((DocConfigDescriptor) contribution);
        } else if ("attributeMapping".equals(extensionPoint)) {
            attributeConfigs.add((AttributeConfigDescriptor) contribution);
        }
    }

    protected ParserConfigRegistry getRegistry() {
        return new ParserConfigRegistry() {

            @Override
            public List<DocConfigDescriptor> getDocCreationConfigs() {
                return docConfigs;
            }

            @Override
            public List<AttributeConfigDescriptor> getAttributConfigs() {
                return attributeConfigs;
            }
        };
    }

    @Override
    public List<DocumentModel> importDocuments(DocumentModel root, File xmlFile)
            throws IOException {
        return importDocuments(root, xmlFile, null);
    }

    @Override
    public List<DocumentModel> importDocuments(DocumentModel root,
            InputStream xmlStream) throws IOException {
        return importDocuments(root, xmlStream, null);
    }

    @Override
    public List<DocumentModel> importDocuments(DocumentModel root, File source,
            Map<String, Object> mvelContext) throws IOException {
        XMLImporterServiceImpl importer = new XMLImporterServiceImpl(root,
                getRegistry(), mvelContext);
        return importer.parse(source);
    }

    @Override
    public List<DocumentModel> importDocuments(DocumentModel root,
            InputStream xmlStream, Map<String, Object> mvelContext)
            throws IOException {
        XMLImporterServiceImpl importer = new XMLImporterServiceImpl(root,
                getRegistry(), mvelContext);
        return importer.parse(xmlStream);
    }

}
