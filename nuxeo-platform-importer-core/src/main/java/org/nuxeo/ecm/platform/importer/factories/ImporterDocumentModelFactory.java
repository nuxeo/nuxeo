package org.nuxeo.ecm.platform.importer.factories;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

public interface ImporterDocumentModelFactory {

    public boolean isTargetDocumentModelFolderish(SourceNode node);

    public DocumentModel createFolderishNode(CoreSession session,
            DocumentModel parent, SourceNode node) throws Exception;

    public DocumentModel createLeafNode(CoreSession session,
            DocumentModel parent, SourceNode node) throws Exception;

}