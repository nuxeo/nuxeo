/*
 * (C) Copyright 2014-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     annejubert
 */

package org.nuxeo.io.fsexporter;

import java.io.File;
import java.io.IOException;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author ajubert
 */
public class FSExporter extends DefaultComponent implements FSExporterService {

    protected FSExporterPlugin exporter = new DefaultExporterPlugin();

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        ExportLogicDescriptor exportLogicDesc = (ExportLogicDescriptor) contribution;
        if (exportLogicDesc.plugin != null) {
            try {
                exporter = exportLogicDesc.plugin.newInstance();
            } catch (IllegalAccessException | InstantiationException e) {
                throw new NuxeoException("Failed to instantiate " + exportLogicDesc.plugin, e);
            }
        }
    }

    @Override
    public void export(CoreSession session, String rootPath, String fspath, String PageProvider) throws IOException {
        DocumentModel root = session.getDocument(new PathRef(rootPath));
        serializeStructure(session, fspath, root, PageProvider);
    }

    private void serializeStructure(CoreSession session, String fsPath, DocumentModel doc, String PageProvider)
            throws IOException {
        File serialized = exporter.serialize(session, doc, fsPath);

        if (doc.isFolder()) {
            DocumentModelList children = exporter.getChildren(session, doc, PageProvider);
            for (DocumentModel child : children) {
                serializeStructure(session, serialized.getAbsolutePath(), child, PageProvider);
            }
        }
    }

    @Override
    public void exportXML(CoreSession session, String rootName, String fileSystemTarget) {
        throw new UnsupportedOperationException();
    }

}
