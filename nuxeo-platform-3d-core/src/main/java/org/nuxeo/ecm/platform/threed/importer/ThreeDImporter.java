package org.nuxeo.ecm.platform.threed.importer;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.platform.filemanager.service.extension.AbstractFileImporter;
import org.nuxeo.ecm.platform.filemanager.utils.FileManagerUtils;
import org.nuxeo.ecm.platform.threed.ThreeDConstants;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;
import java.io.IOException;

public class ThreeDImporter extends AbstractFileImporter {

    public DocumentModel create(CoreSession session, Blob content, String path, boolean overwrite,
                                String fullname, TypeManager typeService) throws IOException {
        DocumentModel container = session.getDocument(new PathRef(path));
        String docType = getDocType(container);
        if (docType == null) {
            docType = getDefaultDocType();
        }
        String title = FileManagerUtils.fetchTitle(content.getFilename());
        DocumentModel doc = session.createDocumentModel(docType);
        doc.setPropertyValue("dc:title", title);
        PathSegmentService pss = Framework.getLocalService(PathSegmentService.class);
        doc.setPathInfo(path, pss.generatePathSegment(doc));
        updateDocument(doc, content);
        return doc;
    }

    @Override
    public String getDefaultDocType() {
        return ThreeDConstants.DOCTYPE;
    }

    @Override
    public boolean isOverwriteByTitle() {
        return true;
    }

}
