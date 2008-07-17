package org.nuxeo.ecm.platform.ui.flex.services;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.flex.javadto.FlexDocumentModel;
import org.nuxeo.ecm.platform.ui.flex.mapping.DocumentModelTranslator;

@Name("flexNavigationContext")
@Scope(ScopeType.SESSION)
public class FlexNavigationContext implements FlexContextManager {

    @In(create=true)
    private transient CoreSession flexDocumentManager;

    private DocumentModel currentDocument;

    @WebRemote
    public FlexDocumentModel getCurrentFlexDocument() throws Exception {
        return DocumentModelTranslator.toFlexType(currentDocument);
    }

    @WebRemote
    public void setCurrentFlexDocument(FlexDocumentModel currentDocument) throws Exception {
        this.currentDocument = DocumentModelTranslator.toDocumentModel(currentDocument, flexDocumentManager);
    }

    public DocumentModel getCurrentDocument() {
        return currentDocument;
    }

    public void setCurrentDocument(DocumentModel currentDocument) {
        this.currentDocument = currentDocument;
    }




}
