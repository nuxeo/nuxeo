package org.nuxeo.template.context.extensions;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.template.api.context.DocumentWrapper;

public class CoreExtensions {

    protected final DocumentModel doc;

    protected final DocumentWrapper nuxeoWrapper;

    public CoreExtensions(DocumentModel doc, DocumentWrapper nuxeoWrapper) {
        this.doc = doc;
        this.nuxeoWrapper = nuxeoWrapper;
    }

    public List<Object> getChildren() throws Exception {
        List<DocumentModel> children = doc.getCoreSession().getChildren(
                doc.getRef());
        List<Object> docs = new ArrayList<Object>();
        for (DocumentModel child : children) {
            docs.add(nuxeoWrapper.wrap(child));
        }
        return docs;
    }

    public Object getParent() throws Exception {
        DocumentRef ref = doc.getParentRef();
        return nuxeoWrapper.wrap(doc.getCoreSession().getDocument(ref));
    }

}
