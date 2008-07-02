package org.nuxeo.ecm.platform.ui.flex.services;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.flex.javadto.FlexDocumentModel;
import org.nuxeo.ecm.platform.ui.flex.mapping.DocumentModelTranslator;

@Name("flexRepositoryService")
@Scope(ScopeType.STATELESS)
public class FlexRepositoryService {

    @In(create=true)
    private transient CoreSession flexDocumentManager;


    @WebRemote
    public FlexDocumentModel getDocumentByPath(String path) throws Exception
    {
        return getDocumentByRef(new PathRef(path));
    }

    @WebRemote
    public FlexDocumentModel getDocumentById(String id) throws Exception
    {
        return getDocumentByRef(new IdRef(id));
    }

    protected FlexDocumentModel getDocumentByRef(DocumentRef docRef) throws Exception
    {
        DocumentModel doc = flexDocumentManager.getDocument(docRef);
        FlexDocumentModel flexDoc = DocumentModelTranslator.toFlexType(doc);
        return flexDoc;
    }


    @WebRemote
    public FlexDocumentModel saveDocument(FlexDocumentModel fdm) throws Exception
    {
        DocumentModel doc = DocumentModelTranslator.toDocumentModel(fdm, flexDocumentManager);
        doc=flexDocumentManager.saveDocument(doc);
        flexDocumentManager.save();
        return DocumentModelTranslator.toFlexType(doc);
    }



}
