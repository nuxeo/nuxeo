/**
 *
 */

package org.nuxeo.io.fsexporter;

import java.io.IOException;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author ajubert
 */
public class FSExporter extends DefaultComponent implements FSExporterService {

    protected FSExporterPlugin exporter = new DefaultExporterPlugin();

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {

        ExportLogicDescriptor exportLogicDesc = (ExportLogicDescriptor) contribution;
        if (exportLogicDesc.plugin != null) {
            exporter = exportLogicDesc.plugin.newInstance();
        }
    }

    @Override
    public void export(CoreSession session, String rootPath, String fsPath,
            boolean ExportDeletedDocuments, String PageProvider) throws ClientException,
            IOException, Exception {
        DocumentModel root = session.getDocument(new PathRef(rootPath));
        serializeStructure(session, fsPath, root, ExportDeletedDocuments, PageProvider);
    }

    private void serializeStructure(CoreSession session, String fsPath,
            DocumentModel doc, boolean ExportDeletedDocuments, String PageProvider)
            throws ClientException, IOException, Exception {

        // serialize(doc, fsPath);
        exporter.serialize(doc, fsPath);

        if (doc.isFolder()) {

            DocumentModelList children = exporter.getChildren(session, doc,
                    ExportDeletedDocuments, PageProvider);

            // getChildrenIterator
            for (DocumentModel child : children) {
                serializeStructure(session, fsPath + "/" + doc.getName(),
                        child, ExportDeletedDocuments, PageProvider);
            }
        }
    }
}
