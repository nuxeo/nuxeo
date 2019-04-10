/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Main Nuxeo Runtime component managing extension points and exposing {@link XMLImporterService}
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class XMLImporterComponent extends DefaultComponent implements XMLImporterService {

    protected List<DocConfigDescriptor> docConfigs = new ArrayList<>();

    protected List<AttributeConfigDescriptor> attributeConfigs = new ArrayList<>();

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if ("documentMapping".equals(extensionPoint)) {
            docConfigs.add((DocConfigDescriptor) contribution);
        } else if ("attributeMapping".equals(extensionPoint)) {
            attributeConfigs.add((AttributeConfigDescriptor) contribution);
        }
    }

    protected ParserConfigRegistry getConfigRegistry() {
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
    public List<DocumentModel> importDocuments(DocumentModel root, File xmlFile) throws IOException {
        return importDocuments(root, xmlFile, null);
    }

    @Override
    public List<DocumentModel> importDocuments(DocumentModel root, File xmlFile, boolean deferSave) throws IOException {
        return importDocuments(root, xmlFile, null, deferSave);
    }

    @Override
    public List<DocumentModel> importDocuments(DocumentModel root, InputStream xmlStream) throws IOException {
        return importDocuments(root, xmlStream, null);
    }

    @Override
    public List<DocumentModel> importDocuments(DocumentModel root, InputStream xmlStream,
            Map<String, Object> mvelContext) throws IOException {
        XMLImporterServiceImpl importer = new XMLImporterServiceImpl(root, getConfigRegistry(), mvelContext, false);
        return importer.parse(xmlStream);
    }

    @Override
    public List<DocumentModel> importDocuments(DocumentModel root, File source, Map<String, Object> mvelContext)
            throws IOException {
        XMLImporterServiceImpl importer = new XMLImporterServiceImpl(root, getConfigRegistry(), mvelContext, false);
        return importer.parse(source);
    }

    @Override
    public List<DocumentModel> importDocuments(DocumentModel root, File source, Map<String, Object> mvelContext,
            boolean deferSave)
            throws IOException {
        XMLImporterServiceImpl importer = new XMLImporterServiceImpl(root, getConfigRegistry(), mvelContext, deferSave);
        return importer.parse(source);
    }
}
