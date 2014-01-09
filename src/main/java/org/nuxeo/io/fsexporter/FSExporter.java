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
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
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
    public void export(CoreSession session, String rootPath, String fspath,
            boolean ExportDeletedDocuments, String PageProvider) throws ClientException,
            IOException, Exception {
        DocumentModel root = session.getDocument(new PathRef(rootPath));
        serializeStructure(session, fspath, root, ExportDeletedDocuments, PageProvider);
    }

    private void serializeStructure(CoreSession session, String fsPath,
            DocumentModel doc, boolean ExportDeletedDocuments, String PageProvider)
            throws ClientException, IOException, Exception {

        exporter.serialize(session, doc, fsPath);

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

    @Override
    public void exportXML(CoreSession session, String rootPath, String fspath) throws ClientException, Exception {
        // TODO Auto-generated method stub
        //
        DocumentModel root = session.getDocument(new PathRef(rootPath));
        DocumentPipe pipe = new DocumentPipeImpl(10);

        /*DocumentTreeReader reader = new DocumentTreeReader(session, root, false);
        pipe.setReader(reader);
        XMLDirectoryWriter writer = new XMLDirectoryWriter(new File(fspath));



        pipe.setWriter(writer);
        pipe.run();*/

    }
}
