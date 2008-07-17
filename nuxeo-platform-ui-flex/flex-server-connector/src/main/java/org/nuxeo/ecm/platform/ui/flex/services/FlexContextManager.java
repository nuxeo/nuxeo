package org.nuxeo.ecm.platform.ui.flex.services;

import org.jboss.seam.annotations.remoting.WebRemote;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.flex.javadto.FlexDocumentModel;

public interface FlexContextManager {

    @WebRemote
    public abstract FlexDocumentModel getCurrentFlexDocument() throws Exception;

    @WebRemote
    public abstract void setCurrentFlexDocument(
            FlexDocumentModel currentDocument) throws Exception;

    public abstract DocumentModel getCurrentDocument();

    public abstract void setCurrentDocument(DocumentModel currentDocument);

}