package org.nuxeo.ecm.platform.ui.flex.services;

import java.util.ArrayList;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
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


    private DocumentRef getRefFromString(String refAsString)
    {
        if (refAsString==null)
            return null;

        if (refAsString.startsWith("/"))
            return new PathRef(refAsString);
        else
            return new IdRef(refAsString);
    }

    @WebRemote
    public FlexDocumentModel getDocument(String refAsString) throws Exception
    {
        DocumentRef ref = getRefFromString(refAsString);
        if (ref==null)
            return null;
        else
            return getDocumentByRef(ref);
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

    @WebRemote
    public FlexDocumentModel createDocumentModel(String parentPath, String type, String name)
    {
        FlexDocumentModel fdm = new FlexDocumentModel();
        fdm.setType(type);
        fdm.setPath(parentPath+"/" + name);
        fdm.setName(name);
        return fdm;
    }


    @WebRemote
    public void deleteDocument(String refAsString) throws ClientException
    {
        DocumentRef docRef = getRefFromString(refAsString);
        if (docRef!=null)
            flexDocumentManager.removeDocument(docRef);
    }


    @WebRemote
    public List<FlexDocumentModel> getChildren(String refAsString) throws Exception
    {
        DocumentRef docRef = getRefFromString(refAsString);
        if (docRef==null)
            return null;

        DocumentModelList children = flexDocumentManager.getChildren(docRef);


        List<FlexDocumentModel> flexChildren = new ArrayList<FlexDocumentModel>();

        for (DocumentModel child : children)
        {
            FlexDocumentModel fdm = DocumentModelTranslator.toFlexTypeFromPrefetch(child);
            flexChildren.add(fdm);
        }
        return flexChildren;
    }

}
