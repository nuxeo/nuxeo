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
    public void export(CoreSession session, String rootPath, String fspath, String PageProvider) throws ClientException,
            IOException, Exception {
        DocumentModel root = session.getDocument(new PathRef(rootPath));
        serializeStructure(session, fspath, root, PageProvider);
    }

    private void serializeStructure(CoreSession session, String fsPath,
            DocumentModel doc, String PageProvider)
            throws ClientException, IOException, Exception {

        exporter.serialize(session, doc, fsPath);

        if (doc.isFolder()) {

            DocumentModelList children = exporter.getChildren(session, doc,
                   PageProvider);

            // getChildrenIterator
            for (DocumentModel child : children) {
                serializeStructure(session, fsPath + "/" + doc.getName(),
                        child,  PageProvider);
            }
        }
    }

    @Override
    public void exportXML(CoreSession session, String rootName,
            String fileSystemTarget) throws ClientException, Exception {
        // TODO Auto-generated method stub
        //
        throw new UnsupportedOperationException();
    }

}
